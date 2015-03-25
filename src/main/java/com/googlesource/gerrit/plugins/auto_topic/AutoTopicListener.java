// Copyright (C) 2015 Christian Rösch
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.googlesource.gerrit.plugins.auto_topic;

import com.google.common.base.Strings;
import com.google.gerrit.common.ChangeListener;
import com.google.gerrit.extensions.annotations.Listen;
import com.google.gerrit.reviewdb.client.Change;
import com.google.gerrit.reviewdb.client.Change.Id;
import com.google.gerrit.reviewdb.server.ReviewDb;
import com.google.gerrit.server.ChangeUtil;
import com.google.gerrit.server.events.ChangeEvent;
import com.google.gerrit.server.events.PatchSetCreatedEvent;
import com.google.gwtorm.server.AtomicUpdate;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Listen
@Singleton
public class AutoTopicListener implements ChangeListener {
  static final Logger log = LoggerFactory.getLogger(AutoTopicListener.class);

  @Inject
  private Provider<ReviewDb> dbProvider;

  @Override
  public void onChangeEvent(ChangeEvent event) {
    if (event instanceof PatchSetCreatedEvent) {
      PatchSetCreatedEvent patchSetCreatedEvent = (PatchSetCreatedEvent) event;
      if (patchSetCreatedEvent.change.topic != null) {
        return;
      }
      if (patchSetCreatedEvent.change.commitMessage != null) {
        Pattern pattern = Pattern.compile("^#([0-9]+).*");
        Matcher matcher =
            pattern.matcher(patchSetCreatedEvent.change.commitMessage);
        if (matcher.find()) {
          final String topic = matcher.group(1);
          Integer intId = Integer.valueOf(patchSetCreatedEvent.change.number);
          Id id = new Change.Id(intId);
          ReviewDb db = dbProvider.get();
          try {
            db.changes().beginTransaction(id);
            try {
              db.changes().atomicUpdate(id, new AtomicUpdate<Change>() {
                @Override
                public Change update(Change change) {
                  change.setTopic(Strings.emptyToNull(topic));
                  ChangeUtil.updated(change);
                  return change;
                }
              });
              db.commit();
            } finally {
              db.rollback();
            }
          } catch (OrmException e) {
            log.debug(e.getMessage(), e);
          }
        }
      }
    }
  }
}
