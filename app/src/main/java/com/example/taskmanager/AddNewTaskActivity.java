package com.example.taskmanager;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class AddNewTaskActivity extends AppCompatActivity {
    private Spinner prioritySpinner;
    private ArrayAdapter<String> priorityAdapter;
    private EditText nameEditText, descriptionEditText, dateEditText;
    private Button saveButton;
    private TaskDao taskDao;
    private TaskDatabase db;
    private String selectedPriorityColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_new_task);
        /*ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

         */
        nameEditText = findViewById(R.id.editTextText);
        descriptionEditText = findViewById(R.id.editTextDescription);
        dateEditText = findViewById(R.id.editTextDate);
        prioritySpinner = findViewById(R.id.spinner);
        saveButton = findViewById(R.id.saveButton);
        db = TaskDatabase.getInstance(this);
        taskDao = db.TaskDao();

        int taskId = getIntent().getIntExtra("task_id", -1);
        if (taskId != -1) {

            loadTask(taskId);
        }
        saveButton.setOnClickListener(v -> {
            saveTask();
        });
        Button cancelButton = findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(v -> {
            startActivity(new Intent(AddNewTaskActivity.this, MainActivity.class));
            finish();
        });
        prioritySpinner = findViewById(R.id.spinner);
        priorityAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, getPriorityList()) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text = (TextView) view;
                String priority = getItem(position);
                setTextColorBasedOnPriority(text, priority);

                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView text = (TextView) view;

                String priority = getItem(position);
                setTextColorBasedOnPriority(text, priority);

                return view;
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
        };

        prioritySpinner.setAdapter(priorityAdapter);

        prioritySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                String priority = parentView.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });
    }

    private List<String> getPriorityList() {
        List<String> priorities = new ArrayList<>();
        priorities.add("Low");
        priorities.add("Medium");
        priorities.add("High");
        return priorities;
    }
    private void saveTask() {
        String taskName = nameEditText.getText().toString();
        String taskDescription = descriptionEditText.getText().toString();
        String taskDate = dateEditText.getText().toString();
        String taskPriority = prioritySpinner.getSelectedItem().toString();

        if (taskName.isEmpty() || taskDescription.isEmpty() || taskDate.isEmpty() || taskPriority.isEmpty()) {
            Toast.makeText(this, "Заповніть всі поля", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseAuth auth = FirebaseAuth.getInstance();
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (userId != null) {
            Task newTask = new Task(taskName, taskDescription, taskDate, taskPriority, userId);
            new Thread(() -> {
                taskDao.insert(newTask);
                runOnUiThread(() -> {
                    String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
                    Intent intent = new Intent(AddNewTaskActivity.this, MainActivity.class);
                    intent.putExtra("EMAIL", email);
                    startActivity(intent);
                    finish();
                });
            }).start();
        } else {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
        }
    }


    private void loadTask(int taskId) {
        new Thread(() -> {
            Task task = taskDao.getTaskById(taskId);

            runOnUiThread(() -> {
                if (task != null) {
                    nameEditText.setText(task.getName());
                    descriptionEditText.setText(task.getDescription());
                    dateEditText.setText(task.getDate());
                    String priorityColor = task.getPriority();
                    ArrayAdapter<CharSequence> adapter = (ArrayAdapter<CharSequence>) prioritySpinner.getAdapter();
                    int position = adapter.getPosition(priorityColor);
                    prioritySpinner.setSelection(position);
                }
            });
        }).start();
    }
}
