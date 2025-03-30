package com.p17142.vroom.listeners;

import com.p17142.vroom.models.User;

@FunctionalInterface
public interface ConversationListener {
    void onConversationClicked(User user);
}
