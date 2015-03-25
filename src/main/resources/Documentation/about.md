This plugin can automatically set the topic of a change according to
an issue number contained in the commit message.

The plugin won't override an existing topic.

Currently the item number is identified by the commit message pattern
`^#([0-9]+).*`.