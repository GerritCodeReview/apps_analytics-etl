package com.gerritforge.analytics.plugin

import com.google.gerrit.sshd.PluginCommandModule

class SshModule extends PluginCommandModule {
  override protected def configureCommands {
    command(classOf[StartCommand])
  }
}
