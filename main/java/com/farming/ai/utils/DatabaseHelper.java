package com.farming.ai.utils;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import androidx.annotation.NonNull;

public class DatabaseHelper {
    private static final DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

    public interface DatabaseCallback {
        void onSuccess();
        void onError(String error);
    }

    public static void createUser(String email, String password, String name, DatabaseCallback callback) {
        String userId = email.replace(".", "_");
        User user = new User(email, password, name);
        
        usersRef.child(userId).setValue(user)
            .addOnSuccessListener(aVoid -> callback.onSuccess())
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public static void loginUser(String email, String password, DatabaseCallback callback) {
        String userId = email.replace(".", "_");
        
        usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null && user.getPassword().equals(password)) {
                        callback.onSuccess();
                    } else {
                        callback.onError("Invalid password");
                    }
                } else {
                    callback.onError("User not found");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }

    public static class User {
        private String email;
        private String password;
        private String name;

        public User() {} // Required for Firebase

        public User(String email, String password, String name) {
            this.email = email;
            this.password = password;
            this.name = name;
        }

        // Getters and setters
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}
