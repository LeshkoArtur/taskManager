package com.example.taskmanager;
import android.os.Bundle;
import android.widget.Switch;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

public class FullTaskActivity extends AppCompatActivity {
    private TaskDao taskDao;
    private Task task;
    private List<Task> taskList;
    private TaskRecyclerViewAdapter adapter;
    private Switch taskSwitch;
    private TextView nameTextView, descriptionTextView, dateTextView, priorityTextView;
    private static final String PREFS_NAME = "taskPrefs";
    private static final String SWITCH_STATE_KEY = "switch_state";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_full_task);


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        int taskId = getIntent().getIntExtra("task_id", -1);
        nameTextView = findViewById(R.id.textViewName);
        descriptionTextView = findViewById(R.id.textViewDescription);
        dateTextView = findViewById(R.id.textViewDate);
        priorityTextView = findViewById(R.id.textViewPriority);
        taskSwitch = findViewById(R.id.switch2);

        TaskDatabase db = TaskDatabase.getInstance(this);
        taskDao = db.TaskDao();
        taskList = new ArrayList<>();
        loadTask(taskId);

        loadSwitchState();
    }

    public void loadTask(int taskId) {
        new Thread(() -> {
            task = taskDao.getTaskById(taskId);
            runOnUiThread(() -> {
                nameTextView.setText(task.getName());
                descriptionTextView.setText(task.getDescription());
                dateTextView.setText(task.getDate());
                String priority = task.getPriority();
                priorityTextView.setText(priority);
                setTextColorBasedOnPriority(priorityTextView, priority);

                boolean switchState = task.isDone();
                taskSwitch.setChecked(switchState);

                taskSwitch.setEnabled(false);
            });
        }).start();
    }

    private void setTextColorBasedOnPriority(TextView text, String priority) {
        switch (priority) {
            case "Low":
                text.setTextColor(getResources().getColor(R.color.low_priority));
                break;
            case "Medium":
                text.setTextColor(getResources().getColor(R.color.medium_priority));
                break;
            case "High":
                text.setTextColor(getResources().getColor(R.color.high_priority));
                break;
            default:
                text.setTextColor(getResources().getColor(R.color.low_priority));
                break;
        }
    }

    private void loadSwitchState() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean switchState = sharedPreferences.getBoolean(SWITCH_STATE_KEY, false);
        taskSwitch.setChecked(switchState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(SWITCH_STATE_KEY, taskSwitch.isChecked());
        editor.apply();
    }
}
