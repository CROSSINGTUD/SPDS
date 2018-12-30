package experiments.google.spreadsheet;

import java.util.Properties;

public class GitRepositoryState {
	final String tags;
	final String branch;
	final String dirty;
	final String remoteOriginUrl;
	final String commitId;
	final String commitIdAbbrev;
	final String describe;
	final String describeShort;
	final String commitUserName;
	final String commitUserEmail;
	final String commitMessageFull;
	final String commitMessageShort;
	final String commitTime;
	final String closestTagName;
	final String closestTagCommitCount;
	final String buildUserName;
	final String buildTime;
	final String buildUserEmail;
	final String buildHost;
	final String buildVersion;
	final String buildNumber;
	final String buildNumberUnique;

	public GitRepositoryState(Properties properties)
	{
	  this.tags = String.valueOf(properties.get("git.tags"));
	  this.branch = String.valueOf(properties.get("git.branch"));
	  this.dirty = String.valueOf(properties.get("git.dirty"));
	  this.remoteOriginUrl = String.valueOf(properties.get("git.remote.origin.url"));

	  this.commitId = String.valueOf(properties.get("git.commit.id")); // OR properties.get("git.commit.id") depending on your configuration
	  this.commitIdAbbrev = String.valueOf(properties.get("git.commit.id.abbrev"));
	  this.describe = String.valueOf(properties.get("git.commit.id.describe"));
	  this.describeShort = String.valueOf(properties.get("git.commit.id.describe-short"));
	  this.commitUserName = String.valueOf(properties.get("git.commit.user.name"));
	  this.commitUserEmail = String.valueOf(properties.get("git.commit.user.email"));
	  this.commitMessageFull = String.valueOf(properties.get("git.commit.message.full"));
	  this.commitMessageShort = String.valueOf(properties.get("git.commit.message.short"));
	  this.commitTime = String.valueOf(properties.get("git.commit.time"));
	  this.closestTagName = String.valueOf(properties.get("git.closest.tag.name"));
	  this.closestTagCommitCount = String.valueOf(properties.get("git.closest.tag.commit.count"));

	  this.buildUserName = String.valueOf(properties.get("git.build.user.name"));
	  this.buildUserEmail = String.valueOf(properties.get("git.build.user.email"));
	  this.buildTime = String.valueOf(properties.get("git.build.time"));
	  this.buildHost = String.valueOf(properties.get("git.build.host"));
	  this.buildVersion = String.valueOf(properties.get("git.build.version"));
	  this.buildNumber = String.valueOf(properties.get("git.build.number"));
	  this.buildNumberUnique = String.valueOf(properties.get("git.build.number.unique"));
	}
}
