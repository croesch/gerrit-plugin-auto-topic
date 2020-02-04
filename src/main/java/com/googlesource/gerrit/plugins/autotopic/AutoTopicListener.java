package com.googlesource.gerrit.plugins.autotopic;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gerrit.extensions.annotations.Listen;
import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.extensions.api.changes.ChangeApi;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.server.data.ChangeAttribute;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.EventListener;
import com.google.gerrit.server.events.PatchSetCreatedEvent;
import com.google.inject.Inject;

@Listen
public class AutoTopicListener implements EventListener {
  private static final Logger log = LoggerFactory.getLogger(AutoTopicListener.class);

  private final GerritApi api;

  private final String[] issuePatterns = {
    "^#([0-9]+).*",                        // issue number pattern (e.g #1234)
    "[\\[{]([a-zA-Z]+-[0-9]+)[\\]}]"       // Jira ticket pattern (e.g [ABC-1234] or {ABC-1234})
  };

  @Inject
  AutoTopicListener(GerritApi api) {
    this.api = api;
  }

  @Override
  public void onEvent(Event event) {
    if (event instanceof PatchSetCreatedEvent) {
      ChangeAttribute change = ((PatchSetCreatedEvent) event).change.get();
      if (change.topic != null) {
        // log.info("topic is: '" + change.topic + "'");
        return;
      }
      if (change.commitMessage == null) {
        // log.info("commit message is null");
        return;
      }
      String topic = null;
      for (String issuePattern : issuePatterns) {
        Pattern pattern = Pattern.compile(issuePattern);
        Matcher matcher = pattern.matcher(change.commitMessage);
        if (matcher.find()) {
          topic = matcher.group(1);
          break;
        }
      }

      if (topic != null) {
        change.topic = topic;
        try {
          ChangeApi changeAPI = api.changes().id(change.project, change.branch, change.id);
          if (changeAPI == null) {
            log.warn("changeAPI is null");
            return;
          }
          changeAPI.topic(topic);
          log.info("Set topic to " + change.topic );
        } catch (RestApiException e) {
          log.error(e.getMessage(), e);
        }
      }
    }
  }
}
