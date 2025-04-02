package com.example.taskmanager;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class TaskRecyclerViewAdapter extends RecyclerView.Adapter<TaskRecyclerViewAdapter.TaskViewHolder> {
    Context context;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private List<Task> tasks;
    private final RecyclerViewInterface recyclerViewInterface;
    private TaskDao taskDao;
    public TaskRecyclerViewAdapter(Context context,List <Task> tasks,RecyclerViewInterface recyclerViewInterface,TaskDao taskDao) {
        this.context = context;
        this.tasks = tasks;
        this.recyclerViewInterface = recyclerViewInterface;
        this.taskDao = taskDao;
    }

    @Override
    public TaskViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_view, parent, false);
        return new TaskViewHolder(view, recyclerViewInterface);
    }

    @Override
    public void onBindViewHolder(TaskViewHolder holder, int position) {
        Task task = tasks.get(position);
        holder.nameTextView.setText(task.getName());
        holder.dateTextView.setText(task.getDate());
        String priority = task.getPriority();
        holder.priorityTextView.setText(priority);
        setTextColorBasedOnPriority(holder.priorityTextView, priority);
        holder.aSwitch.setOnCheckedChangeListener(null);

        // Встановлюємо стан світча відповідно до стану завдання
        holder.aSwitch.setChecked(task.isDone());
        holder.aSwitch.setEnabled(!task.isDone());

        holder.aSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            showConfirmationDialog(holder.aSwitch, task);
        });
    }
    private void showConfirmationDialog(Switch aSwitch, Task task) {
        boolean initialState = task.isDone(); // Початковий стан

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage("Ви впевнені, що хочете змінити статус цього завдання?")
                .setCancelable(false)
                .setPositiveButton("Так", (dialog, id) -> {
                    // Оновлюємо статус у базі даних
                    task.setDone(true); // Завдання виконане
                    executor.execute(() -> {
                        taskDao.update(task);
                        ((Activity) context).runOnUiThread(() -> {
                            aSwitch.setChecked(true);
                            aSwitch.setEnabled(false); // Блокуємо подальші зміни
                            Toast.makeText(context, "Статус змінено", Toast.LENGTH_SHORT).show();
                        });
                    });
                })
                .setNegativeButton("Ні", (dialog, id) -> {
                    // Повертаємо попередній стан без виклику слухача змін
                    aSwitch.setOnCheckedChangeListener(null);
                    aSwitch.setChecked(initialState);
                    aSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        showConfirmationDialog(aSwitch, task);
                    });
                });

        AlertDialog alert = builder.create();
        alert.show();
    }



    @Override
    public int getItemCount() {
        return tasks.size();
    }
    public void updateList(List<Task> newTasks){
        tasks = newTasks;
        notifyDataSetChanged();
    }
    private void setTextColorBasedOnPriority(TextView text, String priority) {
        switch (priority) {
            case "Low":
                text.setTextColor(ContextCompat.getColor(context, R.color.low_priority));
                break;
            case "Medium":
                text.setTextColor(ContextCompat.getColor(context, R.color.medium_priority));
                break;
            case "High":
                text.setTextColor(ContextCompat.getColor(context, R.color.high_priority));
                break;
            default:
                text.setTextColor(ContextCompat.getColor(context, R.color.low_priority));
                break;
        }

}
    public class TaskViewHolder extends RecyclerView.ViewHolder {
        public TextView nameTextView,dateTextView, priorityTextView;
        public Switch aSwitch;

        public TaskViewHolder(View itemView, RecyclerViewInterface recyclerViewInterface) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
            priorityTextView = itemView.findViewById(R.id.priorityTextView);
            aSwitch = itemView.findViewById(R.id.switch1);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (recyclerViewInterface != null){
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION){
                            recyclerViewInterface.onItemClick(position);
                        }
                    }
                }
            });
            itemView.setOnLongClickListener(v -> {
                if (recyclerViewInterface != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        recyclerViewInterface.onItemLongClick(position);
                    }
                }
                return true;
            });
        }
    }
}
