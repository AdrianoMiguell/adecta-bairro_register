package com.miguelprojects.myapplication.room.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.miguelprojects.myapplication.room.dao.CitizenDao
import com.miguelprojects.myapplication.room.dao.UserDao
import com.miguelprojects.myapplication.room.dao.WorkspaceAccessDao
import com.miguelprojects.myapplication.room.dao.WorkspaceDao
import com.miguelprojects.myapplication.room.entity.Citizen
import com.miguelprojects.myapplication.room.entity.User
import com.miguelprojects.myapplication.room.entity.Workspace
import com.miguelprojects.myapplication.room.entity.WorkspaceAccess

@Database(
    entities = [Citizen::class, Workspace::class, User::class, WorkspaceAccess::class],
    version = 3,
    exportSchema = false
)
abstract class MyAppDatabase : RoomDatabase() {
    abstract fun citizenDao(): CitizenDao
    abstract fun userDao(): UserDao
    abstract fun workspaceDao(): WorkspaceDao
    abstract fun workspaceAccessDao(): WorkspaceAccessDao

    companion object {
        @Volatile
        private var INSTANCE: MyAppDatabase? = null

        fun getDatabase(context: Context): MyAppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MyAppDatabase::class.java,
                    "my_app_database"
                )
                    .addMigrations(MIGRATION_2_3)
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            addTriggers(db)
                        }
                    })
//                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Rename the column isPublic to public
        database.execSQL("ALTER TABLE Workspace RENAME COLUMN isPublic TO public")

        // If necessary, you can perform other migration steps here
    }
}

private fun addTriggers(database: SupportSQLiteDatabase) {
    database.execSQL(
        """
        CREATE TRIGGER update_workspace_id_trigger
        AFTER UPDATE OF id ON Workspace
        FOR EACH ROW
        BEGIN
            UPDATE Citizen SET workspaceId = NEW.id WHERE workspaceId = OLD.id;
        END;
    """.trimIndent()
    )

    database.execSQL(
        """
        CREATE TRIGGER update_user_id_in_workspace_access
        AFTER UPDATE OF id ON User
        FOR EACH ROW
        BEGIN
            UPDATE WorkspaceAccess
            SET userId = NEW.id
            WHERE userId = OLD.id;
        END;
    """.trimIndent()
    )

    database.execSQL(
        """
        CREATE TRIGGER update_workspace_id_in_workspace_access
        AFTER UPDATE OF id ON Workspace
        FOR EACH ROW
        BEGIN
            UPDATE WorkspaceAccess
            SET workspaceId = NEW.id
            WHERE workspaceId = OLD.id;
        END;
    """.trimIndent()
    )
}

//         em casos de inconsistencias no database -> .fallbackToDestructiveMigration()
