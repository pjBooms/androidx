/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.room.writer

import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.codegen.VisibilityModifier
import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XFunSpec
import androidx.room.compiler.codegen.XFunSpec.Builder.Companion.addStatement
import androidx.room.compiler.codegen.XTypeSpec
import androidx.room.compiler.codegen.XTypeSpec.Builder.Companion.addOriginatingElement
import androidx.room.compiler.codegen.XTypeSpec.Builder.Companion.addProperty
import androidx.room.compiler.processing.XTypeElement
import androidx.room.ext.RoomMemberNames.DB_UTIL_FOREIGN_KEY_CHECK
import androidx.room.ext.RoomTypeNames
import androidx.room.ext.SQLiteDriverMemberNames
import androidx.room.ext.SQLiteDriverTypeNames.CONNECTION
import androidx.room.migration.bundle.BaseEntityBundle
import androidx.room.migration.bundle.EntityBundle
import androidx.room.migration.bundle.FtsEntityBundle
import androidx.room.vo.AutoMigration

/** Writes the implementation of migrations that were annotated with @AutoMigration. */
class AutoMigrationWriter(
    private val autoMigration: AutoMigration,
    private val dbElement: XTypeElement,
    writerContext: WriterContext,
) : TypeWriter(writerContext) {
    private val addedColumns = autoMigration.schemaDiff.addedColumns
    private val addedTables = autoMigration.schemaDiff.addedTables
    private val renamedTables = autoMigration.schemaDiff.renamedTables
    private val complexChangedTables = autoMigration.schemaDiff.complexChangedTables
    private val deletedTables = autoMigration.schemaDiff.deletedTables

    override fun createTypeSpecBuilder(): XTypeSpec.Builder {
        val builder =
            XTypeSpec.classBuilder(
                codeLanguage,
                autoMigration.getImplTypeName(dbElement.asClassName())
            )
        builder.apply {
            addOriginatingElement(dbElement)
            superclass(RoomTypeNames.MIGRATION)
            // Class is package-protected in Java (no visibility modifier) and internal in Kotlin
            if (language == CodeLanguage.KOTLIN) {
                setVisibility(VisibilityModifier.INTERNAL)
            }
            if (autoMigration.specClassName != null) {
                builder.addProperty(
                    name = "callback",
                    typeName = RoomTypeNames.AUTO_MIGRATION_SPEC,
                    visibility = VisibilityModifier.PRIVATE,
                    initExpr =
                        if (!autoMigration.isSpecProvided) {
                            XCodeBlock.ofNewInstance(codeLanguage, autoMigration.specClassName)
                        } else {
                            null
                        }
                )
            }
            addFunction(createConstructor())
            addFunction(createMigrateMethod())
        }
        return builder
    }

    /**
     * Builds the constructor of the generated AutoMigration.
     *
     * @return The constructor of the generated AutoMigration
     */
    private fun createConstructor(): XFunSpec {
        return XFunSpec.constructorBuilder(codeLanguage, VisibilityModifier.PUBLIC)
            .apply {
                callSuperConstructor(
                    XCodeBlock.of(codeLanguage, "%L", autoMigration.from),
                    XCodeBlock.of(codeLanguage, "%L", autoMigration.to),
                )
                if (autoMigration.isSpecProvided) {
                    addParameter(
                        typeName = RoomTypeNames.AUTO_MIGRATION_SPEC,
                        name = "callback",
                    )
                    addStatement("this.callback = callback")
                }
            }
            .build()
    }

    private fun createMigrateMethod(): XFunSpec {
        val migrateFunctionBuilder: XFunSpec.Builder =
            XFunSpec.builder(
                    language = codeLanguage,
                    name = "migrate",
                    visibility = VisibilityModifier.PUBLIC,
                    isOverride = true,
                )
                .apply {
                    addParameter(
                        typeName = CONNECTION,
                        name = "connection",
                    )
                    addMigrationStatements(this)
                    if (autoMigration.specClassName != null) {
                        addStatement("callback.onPostMigrate(connection)")
                    }
                }
        return migrateFunctionBuilder.build()
    }

    /**
     * Takes the changes provided in the {@link AutoMigrationResult} which are differences detected
     * between the two versions of the same database, and converts them to the appropriate sequence
     * of SQL statements that migrate the database from one version to the other.
     *
     * @param migrateBuilder Builder for the migrate() function to be generated
     */
    private fun addMigrationStatements(migrateBuilder: XFunSpec.Builder) {
        addDropViewStatements(migrateBuilder)
        addSimpleChangeStatements(migrateBuilder)
        addComplexChangeStatements(migrateBuilder)
        addRecreateViewStatements(migrateBuilder)
    }

    /**
     * Adds SQL statements to drop all views of the database in the 'from' version.
     *
     * @param migrateBuilder Builder for the migrate() function to be generated
     */
    private fun addDropViewStatements(migrateBuilder: XFunSpec.Builder) {
        autoMigration.schemaDiff.fromViews.forEach { view ->
            addDatabaseExecuteSqlStatement(migrateBuilder, "DROP VIEW ${view.viewName}")
        }
    }

    /**
     * Adds SQL statements to create all views of the database in the 'to' version.
     *
     * @param migrateBuilder Builder for the migrate() function to be generated
     */
    private fun addRecreateViewStatements(migrateBuilder: XFunSpec.Builder) {
        autoMigration.schemaDiff.toViews.forEach { view ->
            addDatabaseExecuteSqlStatement(migrateBuilder, view.createView())
        }
    }

    /**
     * Adds SQL statements performing schema altering commands that are not directly supported by
     * SQLite (e.g. foreign key changes). These changes are referred to as "complex" changes.
     *
     * @param migrateBuilder Builder for the migrate() function to be generated
     */
    private fun addComplexChangeStatements(migrateBuilder: XFunSpec.Builder) {
        val tablesToCheckForeignKeys = mutableListOf<String>()
        // Create a collection that is sorted such that FTS bundles are handled after the normal
        // tables have been processed
        complexChangedTables.values
            .sortedBy { it.newVersionEntityBundle is FtsEntityBundle }
            .forEach {
                (_, tableNameWithNewPrefix, oldEntityBundle, newEntityBundle, renamedColumnsMap) ->
                if (
                    oldEntityBundle is FtsEntityBundle &&
                        oldEntityBundle.ftsOptions.contentTable.isNotBlank()
                ) {
                    addStatementsToMigrateFtsTable(
                        migrateBuilder,
                        oldEntityBundle,
                        newEntityBundle,
                        renamedColumnsMap
                    )
                } else {
                    addStatementsToCreateNewTable(newEntityBundle, migrateBuilder)
                    addStatementsToContentTransfer(
                        oldEntityBundle.tableName,
                        tableNameWithNewPrefix,
                        oldEntityBundle,
                        newEntityBundle,
                        renamedColumnsMap,
                        migrateBuilder
                    )
                    addStatementsToDropTableAndRenameTempTable(
                        oldEntityBundle.tableName,
                        newEntityBundle.tableName,
                        tableNameWithNewPrefix,
                        migrateBuilder
                    )
                    if (newEntityBundle is EntityBundle) {
                        addStatementsToRecreateIndexes(newEntityBundle, migrateBuilder)
                        if (newEntityBundle.foreignKeys.isNotEmpty()) {
                            tablesToCheckForeignKeys.add(newEntityBundle.tableName)
                        }
                    }
                }
            }
        // Add the SQL statements for checking the foreign key constraints.
        tablesToCheckForeignKeys.forEach { tableName ->
            migrateBuilder.addStatement("%M(connection, %S)", DB_UTIL_FOREIGN_KEY_CHECK, tableName)
        }
    }

    private fun addStatementsToMigrateFtsTable(
        migrateBuilder: XFunSpec.Builder,
        oldTable: BaseEntityBundle,
        newTable: BaseEntityBundle,
        renamedColumnsMap: MutableMap<String, String>
    ) {
        addDatabaseExecuteSqlStatement(migrateBuilder, "DROP TABLE `${oldTable.tableName}`")
        addDatabaseExecuteSqlStatement(migrateBuilder, newTable.createTable())

        val contentTable = (newTable as FtsEntityBundle).ftsOptions.contentTable
        val selectFromTable =
            if (contentTable.isEmpty()) {
                oldTable.tableName
            } else {
                contentTable
            }
        addStatementsToContentTransfer(
            oldTableName = selectFromTable,
            tableNameWithNewPrefix = newTable.tableName,
            oldEntityBundle = oldTable,
            newEntityBundle = newTable,
            renamedColumnsMap = renamedColumnsMap,
            migrateBuilder = migrateBuilder,
            isFtsTableContentTransfer = true
        )
    }

    /**
     * Adds SQL statements performing schema altering commands directly supported by SQLite (adding
     * tables/columns, renaming tables/columns, dropping tables/columns). These changes are referred
     * to as "simple" changes.
     *
     * @param migrateBuilder Builder for the migrate() function to be generated
     */
    private fun addSimpleChangeStatements(migrateBuilder: XFunSpec.Builder) {
        addDeleteTableStatements(migrateBuilder)
        addRenameTableStatements(migrateBuilder)
        addNewColumnStatements(migrateBuilder)
        addNewTableStatements(migrateBuilder)
    }

    /**
     * Adds the SQL statements for creating a new table in the desired revised format of table.
     *
     * @param newTable Schema of the new table to be created
     * @param migrateBuilder Builder for the migrate() function to be generated
     */
    private fun addStatementsToCreateNewTable(
        newTable: BaseEntityBundle,
        migrateBuilder: XFunSpec.Builder
    ) {
        addDatabaseExecuteSqlStatement(migrateBuilder, newTable.createNewTable())
    }

    /**
     * Adds the SQL statements for transferring the contents of the old table to the new version.
     *
     * This function is used in two scenarios: [1] Transfer content after a complex change (in any
     * type of table) has been found where the table has been recreated to reflect the changes and
     * needs the contents transferred from the old table.
     *
     * [2] FTS table content transfers. This needs to be handled separately, in the case where the
     * referenced content table of the FTS table has also undergone a complex change such as a
     * column rename in the same migration. In this scenario, we should be using the most up to date
     * list of columns in the table instead of the column names of the "old" table, since the FTS
     * table content transfer is guaranteed to take place after all complex changes to table
     * structure has completed.
     *
     * @param oldTableName Name of the table in the old version of the database
     * @param tableNameWithNewPrefix Name of the table with the '_new_' prefix added
     * @param oldEntityBundle Entity bundle of the table in the old version of the database
     * @param newEntityBundle Entity bundle of the table in the new version of the database
     * @param renamedColumnsMap Map of the renamed columns of the table (new name -> old name)
     * @param migrateBuilder Builder for the migrate() function to be generated
     */
    private fun addStatementsToContentTransfer(
        oldTableName: String,
        tableNameWithNewPrefix: String,
        oldEntityBundle: BaseEntityBundle,
        newEntityBundle: BaseEntityBundle,
        renamedColumnsMap: MutableMap<String, String>,
        migrateBuilder: XFunSpec.Builder,
        isFtsTableContentTransfer: Boolean = false
    ) {
        val newColumnSequence =
            newEntityBundle.fieldsByColumnName.keys
                .filter {
                    oldEntityBundle.fieldsByColumnName.keys.contains(it) ||
                        renamedColumnsMap.containsKey(it)
                }
                .toMutableList()

        val selectColumnSequence = mutableListOf<String>()
        // Select correct columns for transfer based on whether we are doing an FTS table content
        // transfer or not.
        if (isFtsTableContentTransfer) {
            selectColumnSequence.addAll(newColumnSequence)
            selectColumnSequence.add("rowId")
            newColumnSequence.add("docid")
        } else {
            newColumnSequence.forEach { column ->
                selectColumnSequence.add(renamedColumnsMap[column] ?: column)
            }
        }

        addDatabaseExecuteSqlStatement(
            migrateBuilder,
            buildString {
                append(
                    "INSERT INTO `$tableNameWithNewPrefix` " +
                        "(${newColumnSequence.joinToString(",") { "`$it`" }})" +
                        " SELECT ${selectColumnSequence.joinToString(",") { "`$it`" }} FROM " +
                        "`$oldTableName`",
                )
            }
        )
    }

    /**
     * Adds the SQL statements for dropping the table at the old version and renaming the temporary
     * table to the name of the original table.
     *
     * @param oldTableName Name of the table in the old version of the database
     * @param newTableName Name of the table in the new version of the database
     * @param tableNameWithNewPrefix Name of the table with the '_new_' prefix added
     * @param migrateBuilder Builder for the migrate() function to be generated
     */
    private fun addStatementsToDropTableAndRenameTempTable(
        oldTableName: String,
        newTableName: String,
        tableNameWithNewPrefix: String,
        migrateBuilder: XFunSpec.Builder
    ) {
        addDatabaseExecuteSqlStatement(migrateBuilder, "DROP TABLE `$oldTableName`")
        addDatabaseExecuteSqlStatement(
            migrateBuilder,
            "ALTER TABLE `$tableNameWithNewPrefix` RENAME TO `$newTableName`"
        )
    }

    /**
     * Adds the SQL statements for recreating indexes.
     *
     * @param table The table the indexes of which will be recreated
     * @param migrateBuilder Builder for the migrate() function to be generated
     */
    private fun addStatementsToRecreateIndexes(
        table: EntityBundle,
        migrateBuilder: XFunSpec.Builder
    ) {
        table.indices.forEach { index ->
            addDatabaseExecuteSqlStatement(migrateBuilder, index.getCreateSql(table.tableName))
        }
    }

    /**
     * Adds the SQL statements for removing a table.
     *
     * @param migrateBuilder Builder for the migrate() function to be generated
     */
    private fun addDeleteTableStatements(migrateBuilder: XFunSpec.Builder) {
        deletedTables.forEach { tableName ->
            val deleteTableSql = buildString { append("DROP TABLE `$tableName`") }
            addDatabaseExecuteSqlStatement(migrateBuilder, deleteTableSql)
        }
    }

    /**
     * Adds the SQL statements for renaming a table.
     *
     * @param migrateBuilder Builder for the migrate() function to be generated
     */
    private fun addRenameTableStatements(migrateBuilder: XFunSpec.Builder) {
        renamedTables.forEach { (oldName, newName) ->
            val renameTableSql = buildString {
                append("ALTER TABLE `$oldName` RENAME TO `$newName`")
            }
            addDatabaseExecuteSqlStatement(migrateBuilder, renameTableSql)
        }
    }

    /**
     * Adds the SQL statements for adding new columns to a table.
     *
     * @param migrateBuilder Builder for the migrate() function to be generated
     */
    private fun addNewColumnStatements(migrateBuilder: XFunSpec.Builder) {
        addedColumns.forEach {
            val addNewColumnSql = buildString {
                append(
                    "ALTER TABLE `${it.tableName}` ADD COLUMN `${it.fieldBundle.columnName}` " +
                        it.fieldBundle.affinity
                )
                if (it.fieldBundle.isNonNull) {
                    append(" NOT NULL")
                }
                if (it.fieldBundle.defaultValue?.isNotEmpty() == true) {
                    append(" DEFAULT ${it.fieldBundle.defaultValue}")
                } else {
                    check(!it.fieldBundle.isNonNull) {
                        "A Non-Null field should always have a default value."
                    }
                    append(" DEFAULT NULL")
                }
            }
            addDatabaseExecuteSqlStatement(migrateBuilder, addNewColumnSql)
        }
    }

    /**
     * Adds the SQL statements for adding new tables to a database.
     *
     * @param migrateBuilder Builder for the migrate() function to be generated
     */
    private fun addNewTableStatements(migrateBuilder: XFunSpec.Builder) {
        addedTables.forEach { addedTable ->
            addDatabaseExecuteSqlStatement(migrateBuilder, addedTable.entityBundle.createTable())
            if (addedTable.entityBundle is EntityBundle) {
                addStatementsToRecreateIndexes(addedTable.entityBundle, migrateBuilder)
            }
        }
    }

    /**
     * Adds the given SQL statements into the generated migrate() function to be executed by the
     * database.
     *
     * @param migrateBuilder Builder for the migrate() function to be generated
     * @param sql The SQL statement to be executed by the database
     */
    private fun addDatabaseExecuteSqlStatement(migrateBuilder: XFunSpec.Builder, sql: String) {
        migrateBuilder.addStatement(
            "%L",
            XCodeBlock.ofExtensionCall(
                language = codeLanguage,
                memberName = SQLiteDriverMemberNames.CONNECTION_EXEC_SQL,
                receiverVarName = "connection",
                args = XCodeBlock.of(codeLanguage, "%S", sql)
            )
        )
    }
}
