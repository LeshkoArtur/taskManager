package com.example.taskmanager;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity implements RecyclerViewInterface {
    private RecyclerView recyclerView;
    private TaskRecyclerViewAdapter adapter;
    private TaskDao taskDao;
    private List<Task> taskList;
    private TextView emailTextView;
    private FloatingActionButton addTaskButton;
    private Spinner filterSpinner;
    private FirebaseAuth auth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Button signOutButton = findViewById(R.id.signOutButton);
        auth = FirebaseAuth.getInstance();
        signOutButton.setOnClickListener(v->{
            auth.signOut();
            startActivity(new Intent(MainActivity.this, RegisterLoginActivity.class));
            finish();
        });
        FloatingActionButton addTaskButton = findViewById(R.id.addTaskButton);
        addTaskButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, AddNewTaskActivity.class));
            finish();
        });
        recyclerView = findViewById(R.id.recyclerView);
        addTaskButton = findViewById(R.id.addTaskButton);
        TaskDatabase db = TaskDatabase.getInstance(this);
        taskDao = db.TaskDao();
        taskList = new ArrayList<>();
        adapter = new TaskRecyclerViewAdapter(this,taskList,this,taskDao);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        addTaskButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, AddNewTaskActivity.class));
        });
        SearchView searchView = findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterTasks(newText);
                return false;
            }
        });
        filterSpinner = findViewById(R.id.filterSpinner);
        List<String> filterSortOptions = new ArrayList<>();
        filterSortOptions.add("Priority: Ascending");
        filterSortOptions.add("Priority: Descending");
        filterSortOptions.add("Status: Ascending");
        filterSortOptions.add("Status: Descending");
        filterSortOptions.add("Date: Ascending");
        filterSortOptions.add("Date: Descending");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, filterSortOptions);
        filterSpinner.setAdapter(adapter);
        filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, android.view.View selectedItemView, int position, long id) {
                String selectedOption = parentView.getItemAtPosition(position).toString();
                performFilteringAndSorting(selectedOption);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {

            }
        });
        String email = getIntent().getStringExtra("EMAIL");
        emailTextView = findViewById(R.id.emailTextView);
        emailTextView.setText(email);
        loadTasks();
    }
    private void filterTasks(String newText){
        List<Task> filteredList = new ArrayList<>();
        for (Task task: taskList){
            if (task.getName().toLowerCase().contains(newText.toLowerCase())){
                filteredList.add(task);
            }
        }
        adapter.updateList(filteredList);
    }
    private void loadTasks() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (userId != null) {
            new Thread(() -> {
                List<Task> tasks = taskDao.getAllTasksForUser(userId);
                runOnUiThread(() -> {
                    if (tasks != null && !tasks.isEmpty()) {
                        taskList.clear();
                        taskList.addAll(tasks);
                        adapter.updateList(taskList);
                    }
                });
            }).start();
        }
    }



    @Override
    protected void onResume() {
        super.onResume();
        loadTasks();
    }

    @Override
    public void onItemClick(int position) {
        Task clickedTask = taskList.get(position);
        Intent intent = new Intent(MainActivity.this, FullTaskActivity.class);
        intent.putExtra("task_id", clickedTask.getId());
        startActivity(intent);
    }
    @Override
    public void onItemLongClick(int position) {
        Task selectedTask = taskList.get(position);


        showTaskOptionsDialog(selectedTask);
    }
    private void showTaskOptionsDialog(Task task) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Task Options");
        builder.setMessage("What do you want to do?");


        builder.setPositiveButton("Edit", (dialog, which) -> {
            Intent intent = new Intent(MainActivity.this, AddNewTaskActivity.class);
            intent.putExtra("task_id", task.getId());
            startActivity(intent);
        });


        builder.setNegativeButton("Delete", (dialog, which) -> {
            deleteTask(task);
        });


        builder.setNeutralButton("Cancel", null);

        builder.show();
    }
    private void deleteTask(Task task) {
        new Thread(() -> {
            taskDao.delete(task);
            taskList.remove(task);
            runOnUiThread(() -> {
                adapter.notifyDataSetChanged();
            });
        }).start();
    }private void performFilteringAndSorting(String selectedOption) {
        String[] parts = selectedOption.split(": ");
        String filterType = parts[0];
        String sortDirection = parts[1];

        switch (filterType) {
            case "Priority":
                sortByPriority(sortDirection);
                break;
            case "Status":
                sortByStatus(sortDirection);
                break;
            case "Date":
                sortByDate(sortDirection);
                break;
            default:
                break;
        }
    }

    private void sortByPriority(String direction) {
        taskList.sort((task1, task2) -> {
            int priority1 = getPriorityValue(task1.getPriority());
            int priority2 = getPriorityValue(task2.getPriority());

            return direction.equals("Ascending") ? Integer.compare(priority1, priority2)
                    : Integer.compare(priority2, priority1);
        });
        adapter.notifyDataSetChanged();
    }

    private int getPriorityValue(String priority) {
        switch (priority.toLowerCase()) {
            case "low":
                return 1;
            case "medium":
                return 2;
            case "high":
                return 3;
            default:
                return 0; // Якщо щось неочікуване
        }
    }
    private void sortByStatus(String direction) {
        if (direction.equals("Ascending")) {
            taskList.sort((task1, task2) -> Boolean.compare(task1.isDone, task2.isDone()));
        } else {
            taskList.sort((task1, task2) -> Boolean.compare(task2.isDone(), task1.isDone()));
        }
        adapter.notifyDataSetChanged();
    }

    private void sortByDate(String direction) {
        if (direction.equals("Ascending")) {
            taskList.sort((task1, task2) -> task1.getDate().compareTo(task2.getDate()));
        } else {
            taskList.sort((task1, task2) -> task2.getDate().compareTo(task1.getDate()));
        }
        adapter.notifyDataSetChanged();
    }
}