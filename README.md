# Confession Discord Bot

Anonymous confession Discord bot built with Kotlin, Kord, SQLite, and Exposed ORM.

Supports:

- Anonymous confessions
- Anonymous threaded replies
- Moderator review queues
- Image attachments
- Moderator role delegation
- Automatic thread creation
- Slash commands
- GitHub Actions deployment
- systemd service installation

## Add The Bot

Use the following OAuth2 link to add the bot to your Discord server:

[Add to server](https://discord.com/oauth2/authorize?client_id=1504897516439736501&permissions=8444784025136336&scope=bot%20applications.commands)

## Features

### Anonymous Confessions

Users can submit confessions through:

- `/confess`
- Interactive modal buttons

Confessions are published anonymously while preserving a stable visual identity.

### Anonymous Replies

Replies automatically create and use Discord public threads.

Users can:

- Reply to confessions
- Reply to replies
- Continue anonymous discussions

### Review Queue

Optional moderator approval flow:

- Pending confessions routed to logging channel
- Approve/reject buttons
- Rejection reasons
- Audit metadata
- Moderator attribution

### Image Attachments

Supports remote image URLs and Discord attachments.

Supported formats:

- PNG
- JPEG
- WEBP
- GIF

Images are uploaded as Discord spoiler attachments.

### Moderator Role System

Admins can delegate moderation access to non-admin roles.

Supports:

- Add moderator role
- Remove moderator role
- List moderator roles

### Slash Command Fingerprinting

Global command registration only occurs when command definitions change.

Reduces unnecessary Discord API synchronization.

## Stack

- Kotlin 2.3
- Kord 0.18
- SQLite
- Exposed ORM
- Gradle
- GitHub Actions
- systemd

## Requirements

- Java 21
- Linux server for deployment
- Discord bot token

## Local Development

### Clone Repository

    git clone git@github.com:shalaga44/Confession-Discord-Bot.git

    cd Confession-Discord-Bot

### Configure Environment

Create `.env`

    DISCORD_TOKEN=your_bot_token
    OWNER_ID=your_discord_user_id
    DATABASE_URL=jdbc:sqlite:data/confessions.db

### Run Development Build

    ./gradlew run

### Build Fat Jar

    ./gradlew shadowJar

Generated artifact:

    build/libs/confession-discord-bot.jar

## Commands

### User Commands

#### `/confess`

Submit anonymous confession.

Options:

- `message`
- `image`

Example:

    /confess message:"i regret everything"

#### `/reply`

Reply anonymously to a confession.

Options:

- `confession_id`
- `reply`

Example:

    /reply confession_id:15 reply:"same"

#### `/report`

Report confession for moderator review.

Options:

- `confession_id`

#### `/help`

Display bot help.

Options:

- `section`

Available sections:

- `user`
- `admin`

---

### Admin Commands

#### `/config channel`

Configure confession destination channel.

#### `/config logging`

Configure moderation log channel.

#### `/config review`

Toggle review mode.

#### `/config addmodrole`

Grant moderator access to role.

#### `/config removemodrole`

Remove moderator access from role.

#### `/config listmodroles`

List all authorized moderator roles.

## Recommended Discord Setup

Recommended channels:

- `#confessions`
- `#confession-logs`

Recommended permissions:

### Bot Role

- Send Messages
- Create Public Threads
- Send Messages In Threads
- Embed Links
- Attach Files
- Use Slash Commands
- Read Message History

### Moderation Log Channel

Restrict visibility to moderators only.

## Review Flow

When review mode is enabled:

1. User submits confession
2. Confession stored in database
3. Confession routed to moderation log
4. Moderator approves or rejects
5. Approved confession gets published
6. Rejected confession sends DM notification

## Database

SQLite database location:

    data/confessions.db

Tables:

- `confessions`
- `guild_settings`

## Deployment

## GitHub Actions Deployment

Workflow file:

    .github/workflows/deploy.yml

Required GitHub repository secrets:

- `DEPLOY_HOST`
- `DEPLOY_PORT`
- `DEPLOY_USER`
- `DEPLOY_PATH`
- `DEPLOY_SSH_KEY`

### Deployment Flow

1. Build fat jar
2. SSH into server
3. Rsync repository
4. Install/update systemd service
5. Restart bot service

## systemd Installation

Manual installation:

    sudo bash deploy/install_systemd.sh \
      /opt/confession-discord-bot \
      deploy-user \
      confession-discord-bot

Service file:

    deploy/confession-discord-bot.service

### Service Management

Start:

    sudo systemctl start confession-discord-bot

Restart:

    sudo systemctl restart confession-discord-bot

Status:

    sudo systemctl status confession-discord-bot

Logs:

    journalctl -u confession-discord-bot -f

## Production Environment

Example `.env`:

    DISCORD_TOKEN=your_bot_token
    OWNER_ID=your_discord_user_id
    DATABASE_URL=jdbc:sqlite:data/confessions.db

## Project Structure

    src/main/kotlin/dev/shalaga44/
    ├── bot/
    ├── commands/
    ├── config/
    ├── embeds/
    ├── models/
    ├── moderation/
    ├── services/
    ├── storage/
    └── util/

## Moderation Rules

Current automatic moderation blocks:

- `discord.gg/`
- `@everyone`
- `@here`

Spam detection currently blocks:

- Messages larger than 4000 characters

## Security Notes

- Keep moderation logs private
- Never expose author IDs
- Restrict bot permissions
- Restrict logging channel visibility
- Store `.env` securely
- Never commit production tokens

## License

Apache License 2.0