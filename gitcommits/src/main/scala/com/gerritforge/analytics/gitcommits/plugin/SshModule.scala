package com.gerritforge.analytics.gitcommits.plugin

import com.google.gerrit.sshd.PluginCommandModule

class SshModule extends PluginCommandModule {
  override protected def configureCommands {
    command(classOf[ProcessGitCommitsCommand])
  }
}
