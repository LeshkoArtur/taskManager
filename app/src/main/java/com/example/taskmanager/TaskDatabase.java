package com.example.taskmanager;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {Task.class}, version = 2)
public abstract class TaskDatabase extends RoomDatabase {

    public abstract TaskDao TaskDao();

    private static volatile TaskDatabase taskDatabase;

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE tasks ADD COLUMN userId TEXT");
        }
    };

    public static TaskDatabase getInstance(Context context) {

        if (taskDatabase == null) {
            synchronized (TaskDatabase.class) {
                if (taskDatabase == null) {
                    // Використовуємо context.getApplicationContext() для створення бази даних
                    taskDatabase = Room.databaseBuilder(context.getApplicationContext(),
                                    TaskDatabase.class, "tasks")
                            .addMigrations(MIGRATION_1_2)  // Додаємо міграцію
                            .fallbackToDestructiveMigration()  // Видалення БД при зміні версії
                            .build();
                }
            }
        }
        return taskDatabase;
    }
}
