package org.greenrobot.eventbus;

  public class EventBus {
      private static final EventBus DEFAULT = new EventBus();
      public static EventBus getDefault() { return DEFAULT; }
      public void register(Object subscriber) {}
      public void unregister(Object subscriber) {}
      public void post(Object event) {}
      public void postSticky(Object event) {}
      public boolean isRegistered(Object subscriber) { return false; }
  }
  