package dev.shalaga44.bot

import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.interaction.modal
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.GuildChannel
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.entity.channel.MessageChannel
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.interaction.ButtonInteraction
import dev.kord.core.entity.interaction.GuildInteraction
import dev.kord.core.entity.interaction.Interaction
import dev.kord.core.entity.interaction.SubCommand
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.guild.GuildCreateEvent
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.event.interaction.ModalSubmitInteractionCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.component.actionRow
import dev.kord.rest.builder.component.*
import dev.kord.rest.builder.message.embed
import dev.shalaga44.embeds.ConfessionEmbedFactory
import dev.shalaga44.embeds.HelpEmbedFactory
import dev.shalaga44.services.ConfessionService
import dev.shalaga44.services.GuildConfigService
import dev.shalaga44.services.ModerationService
import dev.shalaga44.services.ReplyService
import dev.shalaga44.util.displayColor
import dev.shalaga44.util.stableEmojiIdentity
import dev.shalaga44.util.ImageUtil
import dev.shalaga44.util.CrashReporter
import io.ktor.client.request.forms.*
import dev.kord.rest.request.KtorRequestException
import java.time.Instant

private val Interaction.guildId: Snowflake?
    get() {
        return (this as? GuildInteraction)?.guildId
    }

class EventRegistrar(
    private val kord: Kord,
    ownerId: Long
) {

    private val confessionService = ConfessionService()
    private val crashReporter =
        CrashReporter(
            kord = kord,
            ownerId = ownerId
        )
    private val moderationService = ModerationService()
    private val guildConfigService = GuildConfigService()
    private val replyService = ReplyService(confessionService)

    private suspend fun resolveMemberColor(
        guildId: Long,
        authorId: Long
    ) = kord.getUser(
        Snowflake(authorId)
    )
        ?.asMember(Snowflake(guildId))
        ?.displayColor()

    suspend fun register() {
        registerReadyListener()
        registerGuildJoinListener()
        registerInteractionListener()
        registerButtonListener()
        registerModalListener()
    }

    private suspend fun registerReadyListener() {
        kord.on<ReadyEvent> {
            println("Connected as ${self.username}")
        }
    }

    private suspend fun registerGuildJoinListener() {
        return
        kord.on<GuildCreateEvent> {

            runCatching {
                val owner =
                    kord.getUser(guild.ownerId)
                        ?: return@runCatching

                val dmChannel =
                    owner.getDmChannel()

                dmChannel.createMessage {
                    content =
                        """
                        Thanks for adding confession-discord-bot to "${guild.name}".
                        
                        Run `/help section:admin` inside your server anytime for setup instructions.
                        """.trimIndent()

                    embed {
                        HelpEmbedFactory
                            .buildAdminHelpEmbed()
                            .invoke(this)
                    }
                }
            }.onFailure {
                println(
                    "Failed to send onboarding help for guild ${guild.id.value}: ${it.message}"
                )
            }
        }
    }

    private suspend fun registerInteractionListener() {
        kord.on<ChatInputCommandInteractionCreateEvent> {
            runCatching {
                when (interaction.command.rootName) {
                    "confess" -> handleConfession()
                    "reply" -> handleReply()
                    "report" -> handleReport()
                    "config" -> handleConfig()
                    "help" -> handleHelp()
                }
            }.onFailure {
                println(
                    "Interaction command failed: ${interaction.command.rootName} - ${it.message}"
                )

                it.printStackTrace()

                crashReporter.report(
                    context =
                        """
                        Event Type: ChatInputCommandInteractionCreateEvent
                        Command: ${interaction.command.rootName}
                        Guild: ${interaction.guildId?.value}
                        User: ${interaction.user.id.value}
                        """.trimIndent(),
                    throwable = it
                )
            }
        }
    }

    private suspend fun registerButtonListener() {
        kord.on<ButtonInteractionCreateEvent> {
            runCatching {
                val componentId = interaction.componentId

                when {
                    componentId.startsWith("approve:") -> {
                        handleApprove(componentId)
                    }

                    componentId.startsWith("approve-reply:") -> {
                        handleApproveReply(componentId)
                    }

                    componentId.startsWith("reject:") -> {
                        handleRejectPrompt(componentId)
                    }

                    componentId.startsWith("reply-thread:") -> {
                        handleReplyThread(componentId)
                    }

                    componentId == "submit-confession" -> {
                        interaction.modal(
                            title = "Submit Anonymous Confession",
                            customId = "submit-confession-modal"
                        ) {
                            actionRow {
                                textInput(
                                    style = dev.kord.common.entity.TextInputStyle.Paragraph,
                                    customId = "confession-content",
                                    label = "Confession"
                                ) {
                                    required = true
                                    placeholder =
                                        "Write your anonymous confession"
                                    allowedLength = 1..4000
                                }
                            }

                            actionRow {
                                textInput(
                                    style = dev.kord.common.entity.TextInputStyle.Short,
                                    customId = "confession-image-url",
                                    label = "Attachment URL (optional)"
                                ) {
                                    required = false
                                    placeholder =
                                        "https://example.com/image.png"
                                }
                            }
                        }
                    }

                    else -> {}
                }
            }.onFailure {
                println(
                    "Button interaction failed: ${interaction.componentId} - ${it.message}"
                )

                it.printStackTrace()

                crashReporter.report(
                    context =
                        """
                        Event Type: ButtonInteractionCreateEvent
                        Component ID: ${interaction.componentId}
                        Guild: ${interaction.guildId?.value}
                        User: ${interaction.user.id.value}
                        """.trimIndent(),
                    throwable = it
                )

                runCatching {
                    interaction.respondEphemeral {
                        content =
                            "An internal error occurred while processing this action."
                    }
                }
            }
        }
    }

    private suspend fun registerModalListener() {
        kord.on<ModalSubmitInteractionCreateEvent> {
            when {
                interaction.modalId == "submit-confession-modal" -> {
                    handleSubmitConfessionModal()
                }

                interaction.modalId.startsWith("reply-modal:") -> {
                    handleReplyModal(interaction.modalId)
                }

                interaction.modalId.startsWith("reject-modal:") -> {
                    handleRejectModal(interaction.modalId)
                }
            }
        }
    }

    private suspend fun ChatInputCommandInteractionCreateEvent.requireAdmin(): Boolean {
        val guildInteraction =
            interaction as? GuildInteraction
                ?: return false

        val member =
            interaction.user.asMember(guildInteraction.guildId)

        val permissions = member.getPermissions()

        if (permissions.values.contains(Permission.Administrator)) {
            return true
        }

        val settings =
            guildConfigService.getSettings(
                guildInteraction.guildId.value.toLong()
            )

        val authorizedRoles =
            settings?.moderatorRoleIds
                ?.toSet()
                ?: emptySet()

        val hasAuthorizedRole =
            member.roleIds.any {
                it.value.toLong() in authorizedRoles
            }

        if (!hasAuthorizedRole) {
            interaction.respondEphemeral {
                content =
                    "You do not have permission to use this command."
            }

            return false
        }

        return true
    }

    private suspend fun ButtonInteractionCreateEvent.requireModeratorAccess(): Boolean {
        val guildId =
            (interaction as? GuildInteraction)?.guildId
                ?.value
                ?.toLong()
                ?: return false

        val member =
            interaction.user.asMember(interaction.guildId!!)

        val permissions = member.getPermissions()

        if (permissions.values.contains(Permission.Administrator)) {
            return true
        }

        val settings =
            guildConfigService.getSettings(guildId)

        val authorizedRoles =
            settings?.moderatorRoleIds
                ?.toSet()
                ?: emptySet()

        val hasAuthorizedRole =
            member.roleIds.any {
                it.value.toLong() in authorizedRoles
            }

        if (!hasAuthorizedRole) {
            interaction.respondEphemeral {
                content =
                    "You do not have permission to perform this action."
            }

            return false
        }

        return true
    }

    private suspend fun ButtonInteractionCreateEvent.handleApprove(
        componentId: String
    ) {
        if (!requireModeratorAccess()) {
            return
        }

        val confessionId =
            componentId.removePrefix("approve:")
                .toIntOrNull()
                ?: return

        val confession =
            confessionService.getConfessionById(
                confessionId = confessionId.toInt())
                ?: return

        val channel = kord.getChannel(
            Snowflake(confession.channelId)
        )
            ?.asChannelOf<MessageChannel>()
            ?: return

        val publishedMessage = channel.createMessage {
            confession.imageUrl?.let { imageUrl ->
                val imageBytes =
                    java.net.URL(imageUrl)
                        .openStream()
                        .readBytes()

                val spoilerFilename =
                    ImageUtil.spoilerFilename(
                        prefix = "confession",
                        id = confession.id,
                        imageUrl = imageUrl
                    )

                addFile(
                    name = spoilerFilename,
                    contentProvider = ChannelProvider {
                        io.ktor.utils.io.ByteReadChannel(imageBytes)
                    }
                )
            }

            embed {
                val guildId =
                    (channel as? GuildChannel)
                        ?.guildId
                        ?.value
                        ?.toLong()

                val embedColor =
                    guildId?.let {
                        resolveMemberColor(
                            guildId = it,
                            authorId = confession.authorId
                        )
                    }

                ConfessionEmbedFactory
                    .buildConfessionEmbed(
                                confessionId = confession.publicConfessionId,
                        content = confession.content,
                        authorId = confession.authorId,
                        embedColor = embedColor
                    )
                    .invoke(this)
            }

            actionRow {
                interactionButton(
                    style = dev.kord.common.entity.ButtonStyle.Secondary,
                    customId = "reply-thread:pending"
                ) {
                    label = "Reply"
                }
            }
        }

        publishedMessage.edit {
            actionRow {
                interactionButton(
                    style = dev.kord.common.entity.ButtonStyle.Primary,
                    customId = "submit-confession"
                ) {
                    label = "Submit a Confession!"
                }

                interactionButton(
                    style = dev.kord.common.entity.ButtonStyle.Secondary,
                    customId = "reply-thread:${publishedMessage.id.value}"
                ) {
                    label = "Reply"
                }
            }
        }

        confessionService.updateMessageId(
            confessionId = confession.publicConfessionId,
            messageId = publishedMessage.id.value.toLong()
        )

        val reviewedAt =
            "<t:${Instant.now().epochSecond}:F>"

        interaction.message.edit {
            embed {
                ConfessionEmbedFactory
                    .buildConfessionEmbed(
                        confessionId = confession.publicConfessionId,
                        content = confession.content,
                        authorId = confession.authorId,
                        embedColor = resolveMemberColor(
                            guildId = confession.guildId,
                            authorId = confession.authorId
                        )
                    )
                    .invoke(this)

                field {
                    name = "Reviewed By"
                    value = interaction.user.mention
                    inline = true
                }

                field {
                    name = "Reviewed At"
                    value = reviewedAt
                    inline = true
                }

                footer {
                    text = "Approved"
                }
            }

            components = mutableListOf()
        }

        val jumpUrl =
            "https://discord.com/channels/${confession.guildId}/${confession.channelId}/${publishedMessage.id.value}"

        interaction.respondEphemeral {
            content =
                "Confession #${confession.publicConfessionId} approved and published.\n$jumpUrl"

        }
    }

    private suspend fun ButtonInteractionCreateEvent.handleApproveReply(
        componentId: String
    ) {
        if (!requireModeratorAccess()) {
            return
        }

        val replyId =
            componentId.removePrefix("approve-reply:")
                .toIntOrNull()
                ?: return

        val reply =
            confessionService.getConfessionById(
                confessionId = replyId
            )
                ?: return

        val parentConfessionId =
            reply.parentConfessionId
                ?: return

        val parentConfession =
            confessionService.getConfessionById(
                confessionId = parentConfessionId
            )
                ?: return

        val thread =
            ensureReplyThread(parentConfession)
                ?: return

        val targetMessageId =
            if (parentConfession.channelId == thread.id.value.toLong()) {
                parentConfession.messageId
            } else {
                null
            }

        val replyMessage = thread.createMessage {
            targetMessageId?.let {
                messageReference = Snowflake(it)
            }

            reply.imageUrl?.let { imageUrl ->
                val imageBytes =
                    java.net.URL(imageUrl)
                        .openStream()
                        .readBytes()

                val spoilerFilename =
                    ImageUtil.spoilerFilename(
                        prefix = "reply",
                        id = reply.id,
                        imageUrl = imageUrl
                    )

                addFile(
                    name = spoilerFilename,
                    contentProvider = ChannelProvider {
                        io.ktor.utils.io.ByteReadChannel(imageBytes)
                    }
                )
            }

            embed {
                ConfessionEmbedFactory
                    .buildReplyEmbed(
                        confessionId = parentConfession.publicConfessionId,
                        content = reply.content,
                        authorId = reply.authorId,
                        embedColor = resolveMemberColor(
                            guildId = reply.guildId,
                            authorId = reply.authorId
                        )
                    )
                    .invoke(this)

                reply.imageUrl?.let { imageUrl ->
                    val spoilerFilename =
                        ImageUtil.spoilerFilename(
                            prefix = "reply",
                            id = reply.id,
                            imageUrl = imageUrl
                        )

                    image = "attachment://$spoilerFilename"
                }
            }
        }

        replyMessage.edit {
            actionRow {
                interactionButton(
                    style = dev.kord.common.entity.ButtonStyle.Primary,
                    customId = "submit-confession"
                ) {
                    label = "Submit a Confession!"
                }

                interactionButton(
                    style = dev.kord.common.entity.ButtonStyle.Secondary,
                    customId = "reply-thread:${replyMessage.id.value}"
                ) {
                    label = "Reply"
                }
            }
        }

        confessionService.updateMessageId(
            confessionId = reply.id,
            messageId = replyMessage.id.value.toLong()
        )

        val reviewedAt =
            "<t:${Instant.now().epochSecond}:F>"

        interaction.message.edit {
            embed {
                ConfessionEmbedFactory
                    .buildReplyEmbed(
                        confessionId = parentConfession.id,
                        content = reply.content,
                        authorId = reply.authorId,
                        titleText = "Approved Anonymous Reply (#${parentConfession.publicConfessionId})",
                        embedColor = resolveMemberColor(
                            guildId = reply.guildId,
                            authorId = reply.authorId
                        )
                    )
                    .invoke(this)

                field {
                    name = "Reviewed By"
                    value = interaction.user.mention
                    inline = true
                }

                field {
                    name = "Reviewed At"
                    value = reviewedAt
                    inline = true
                }

                footer {
                    text = "Approved"
                }
            }

            components = mutableListOf()
        }

        interaction.respondEphemeral {
            content =
                "Reply for confession #${parentConfession.publicConfessionId} approved and published."
        }
    }

    private suspend fun ButtonInteractionCreateEvent.handleReplyThread(
        componentId: String
    ) {
        val messageId =
            componentId.removePrefix("reply-thread:")
                .toLongOrNull()
                ?: return

        interaction.modal(
            title = "Submit a Reply",
            customId = "reply-modal:$messageId"
        ) {
            actionRow {
                textInput(
                    style = dev.kord.common.entity.TextInputStyle.Paragraph,
                    customId = "reply-content",
                    label = "Reply"
                ) {
                    required = true
                    placeholder =
                        "Write your anonymous reply"
                    allowedLength = 0..2000
                }
            }

            actionRow {
                textInput(
                    style = dev.kord.common.entity.TextInputStyle.Short,
                    customId = "reply-message-id",
                    label = "Message To Reply To"
                ) {
                    required = false
                    value = messageId.toString()
                    placeholder =
                        "Message ID or Discord message link"
                }
            }

            actionRow {
                textInput(
                    style = dev.kord.common.entity.TextInputStyle.Short,
                    customId = "reply-attachment-url",
                    label = "Attachment URL (optional)"
                ) {
                    required = false
                    placeholder =
                        "https://example.com/image.png"
                }
            }
        }
    }

    private suspend fun ModalSubmitInteractionCreateEvent.handleReplyModal(
        modalId: String
    ) {
        val defaultMessageId =
            modalId.removePrefix("reply-modal:")
                .toLongOrNull()
                ?: return

        val components =
            interaction.textInputs

        val replyContent =
            components["reply-content"]?.value
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: run {
                    interaction.respondEphemeral {
                        content = "Reply content is required."
                    }

                    return
                }

        val messageField =
            components["reply-message-id"]?.value
                ?.trim()

        val resolvedMessageId =
            messageField
                ?.substringAfterLast("/")
                ?.toLongOrNull()
                ?: defaultMessageId

        val attachmentUrl =
            components["reply-attachment-url"]?.value
                ?.trim()
                ?.takeIf { it.isNotBlank() }

        val confession =
            confessionService.getConfessionByMessageId(
                guildId = interaction.guildId!!.value.toLong(),
                messageId = resolvedMessageId
            )
                ?: run {
                    interaction.respondEphemeral {
                        content = "Unable to resolve reply target."
                    }

                    return
                }

        val (_, content) = replyService.buildReply(
            guildId = confession.guildId,
            confessionId = confession.publicConfessionId,
            content = replyContent
        )

        val thread =
            ensureReplyThread(confession)
                ?: run {
                    interaction.respondEphemeral {
                        this.content = "Unable to create reply thread."
                    }

                    return
                }

        val settings =
            guildConfigService.getSettings(confession.guildId)

        val replyRecord =
            confessionService.createConfession(
                guildId = confession.guildId,
                channelId = thread.id.value.toLong(),
                authorId = interaction.user.id.value.toLong(),
                content = content,
                imageUrl = attachmentUrl,
                parentConfessionId = confession.id,
                messageId = 0L
            )

        if (settings?.reviewEnabled == true) {
            val loggingChannelId =
                settings.loggingChannelId

            if (loggingChannelId != null) {
                val loggingChannel =
                    kord.getChannel(
                        Snowflake(loggingChannelId)
                    )
                        ?.asChannelOf<MessageChannel>()

                loggingChannel?.createMessage {
                    attachmentUrl?.let { resolvedImageUrl ->
                        val imageBytes =
                            java.net.URL(resolvedImageUrl)
                                .openStream()
                                .readBytes()

                        val spoilerFilename =
                            ImageUtil.spoilerFilename(
                                prefix = "reply-review",
                                id = replyRecord.id,
                                imageUrl = resolvedImageUrl
                            )

                        addFile(
                            name = spoilerFilename,
                            contentProvider = ChannelProvider {
                                io.ktor.utils.io.ByteReadChannel(imageBytes)
                            }
                        )
                    }

                    embed {
                        ConfessionEmbedFactory
                            .buildReplyEmbed(
                                confessionId = confession.publicConfessionId,
                                content = content,
                                authorId = confession.authorId,
                                titleText = "Pending Anonymous Reply (#${confession.publicConfessionId})",
                                embedColor = resolveMemberColor(
                                    guildId = confession.guildId,
                                    authorId = confession.authorId
                                )
                            )
                            .invoke(this)

                        field {
                            name = "Replying To"
                            value =
                                confession.content
                                    .take(1024)
                        }

                        footer {
                            text = "Pending Moderator Review"
                        }
                    }

                    actionRow {
                        interactionButton(
                            style = dev.kord.common.entity.ButtonStyle.Success,
                            customId = "approve-reply:${replyRecord.id}"
                        ) {
                            label = "Approve Reply"
                        }

                        interactionButton(
                            style = dev.kord.common.entity.ButtonStyle.Danger,
                            customId = "reject:${replyRecord.id}"
                        ) {
                            label = "Reject Reply"
                        }
                    }
                }
            }

            interaction.respondEphemeral {
                this.content =
                    "Anonymous reply submitted for moderator review."
            }

            return
        }

        val targetMessageId =
            if (confession.channelId == thread.id.value.toLong()) {
                confession.messageId
            } else {
                null
            }

        val replyMessage = thread.createMessage {
            targetMessageId?.let {
                messageReference = Snowflake(it)
            }

            attachmentUrl?.let { imageUrl ->
                val imageBytes =
                    java.net.URL(imageUrl)
                        .openStream()
                        .readBytes()

                val spoilerFilename =
                    ImageUtil.spoilerFilename(
                        prefix = "reply",
                        id = replyRecord.id,
                        imageUrl = imageUrl
                    )

                addFile(
                    name = spoilerFilename,
                    contentProvider = ChannelProvider {
                        io.ktor.utils.io.ByteReadChannel(imageBytes)
                    })
            }

            embed {
                ConfessionEmbedFactory
                    .buildReplyEmbed(
                        confessionId = confession.id,
                        content = content,
                        authorId = confession.authorId,
                        embedColor = resolveMemberColor(
                            guildId = confession.guildId,
                            authorId = confession.authorId
                        )
                    )
                    .invoke(this)

                attachmentUrl?.let { imageUrl ->
                    val spoilerFilename =
                        ImageUtil.spoilerFilename(
                            prefix = "reply",
                            id = replyRecord.id,
                            imageUrl = imageUrl
                        )

                    image = "attachment://$spoilerFilename"
                }
            }
        }

        replyMessage.edit {
            actionRow {
                interactionButton(
                    style = dev.kord.common.entity.ButtonStyle.Secondary,
                    customId = "reply-thread:${replyMessage.id.value}"
                ) {
                    label = "Reply"
                }
            }
        }

        confessionService.updateMessageId(
            confessionId = replyRecord.id,
            messageId = replyMessage.id.value.toLong()
        )

        interaction.respondEphemeral {
            this.content =
                "Anonymous reply delivered to confession #${confession.publicConfessionId}."
        }
    }

    private suspend fun ButtonInteractionCreateEvent.handleRejectPrompt(
        componentId: String
    ) {
        if (!requireModeratorAccess()) {
            return
        }

        val confessionId =
            componentId.removePrefix("reject:")
                .toIntOrNull()
                ?: return

        interaction.modal(
            title = "Reject Confession",
            customId = "reject-modal:$confessionId"
        ) {
            actionRow {
                textInput(
                    style = dev.kord.common.entity.TextInputStyle.Paragraph,
                    customId = "reject-reason",
                    label = "Reason For Rejection"
                ) {
                    required = true
                    placeholder =
                        "Explain why this confession was rejected"
                    allowedLength = 1..1000
                }
            }
        }
    }

    private suspend fun ModalSubmitInteractionCreateEvent.handleRejectModal(
        modalId: String
    ) {
        val confessionId =
            modalId.removePrefix("reject-modal:")
                .toLongOrNull()
                ?: return

        val confession =
            confessionService.getConfessionById(
                confessionId = confessionId.toInt()
            )
                ?: return

        val reason =
            interaction.textInputs["reject-reason"]
                ?.value
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: run {
                    interaction.respondEphemeral {
                        content = "Rejection reason is required."
                    }

                    return
                }

        val reviewedAt =
            "<t:${Instant.now().epochSecond}:F>"

        interaction.message?.edit {
            embed {
                ConfessionEmbedFactory
                    .buildConfessionEmbed(
                        confessionId = confession.id,
                        content = confession.content,
                        authorId = confession.authorId,
                        embedColor = resolveMemberColor(
                            guildId = confession.guildId,
                            authorId = confession.authorId
                        )
                    )
                    .invoke(this)

                field {
                    name = "Reviewed By"
                    value = interaction.user.mention
                    inline = true
                }

                field {
                    name = "Reviewed At"
                    value = reviewedAt
                    inline = true
                }

                field {
                    name = "Reason"
                    value = reason
                }

                footer {
                    text = "Rejected"
                }
            }

            components = mutableListOf()
        }

        runCatching {
            val user =
                kord.getUser(
                    Snowflake(confession.authorId)
                )

            val location =
                "<#${confession.channelId}>"

            user?.getDmChannel()
                ?.createMessage {
                    embed {
                        title = "Confession Denied"
                        description =
                            """
                            Your confession has been denied in $location.

                            "${confession.content}"
                            """.trimIndent()

                        field {
                            name = "Reason"
                            value = reason
                        }

                        color = dev.kord.common.Color(0xED4245)

                        footer {
                            text = "Confession #${confession.publicConfessionId}"
                        }
                    }
                }
        }

        interaction.respondEphemeral {
            content =
                "Confession #${confession.publicConfessionId} rejected."
        }
    }

    private suspend fun ChatInputCommandInteractionCreateEvent.handleConfession() {
        val guildId =
            (interaction as? GuildInteraction)?.guildId?.value?.toLong()
                ?: return

        val message =
            interaction.command.strings["message"]
                ?: return

        val imageUrl =
            interaction.command.attachments["image"]
                ?.url

        submitConfession(
            guildId = guildId,
            authorId = interaction.user.id.value.toLong(),
            content = message,
            imageUrl = imageUrl,
            sourceChannelId = interaction.channelId.value.toLong(),
            sourceChannelName =
                interaction.channel.asChannel().data.name.value
                    ?: "unknown-channel",
            responder = {
                interaction.respondEphemeral {
                    this.content = it
                }
            }
        )
    }

    private suspend fun ModalSubmitInteractionCreateEvent.handleSubmitConfessionModal() {
        val guildId =
            interaction.guildId?.value?.toLong()
                ?: return

        val content =
            interaction.textInputs["confession-content"]
                ?.value
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: run {
                    interaction.respondEphemeral {
                        this.content = "Confession content is required."
                    }

                    return
                }

        val imageUrl =
            interaction.textInputs["confession-image-url"]
                ?.value
                ?.trim()
                ?.takeIf { it.isNotBlank() }

        submitConfession(
            guildId = guildId,
            authorId = interaction.user.id.value.toLong(),
            content = content,
            imageUrl = imageUrl,
            sourceChannelId = interaction.channelId.value.toLong(),
            sourceChannelName =
                interaction.channel.asChannel().data.name.value
                    ?: "unknown-channel",
            responder = {
                interaction.respondEphemeral {
                    this.content = it
                }
            }
        )
    }

    private suspend fun submitConfession(
        guildId: Long,
        authorId: Long,
        content: String,
        imageUrl: String?,
        sourceChannelId: Long,
        sourceChannelName: String,
        responder: suspend (String) -> Unit
    ) {
        val moderation =
            moderationService.moderate(content)

        if (!moderation.accepted) {
            responder(
                moderation.reason ?: "Message rejected"
            )

            return
        }

        val settings =
            guildConfigService.getSettings(guildId)

        if (settings == null) {
            responder(
                """
                This server has not configured the confession bot yet.
                
                An administrator must run:
                `/config channel`
                """.trimIndent()
            )

            return
        }

        val channelId =
            settings.confessionChannelId

        if (settings.reviewEnabled &&
            settings.loggingChannelId == null
        ) {
            responder(
                """
                Review mode is enabled but no moderation logging channel is configured.
                
                An administrator must run:
                `/config logging`
                """.trimIndent()
            )

            return
        }

        val confession =
            confessionService.createConfession(
                guildId = guildId,
                channelId = channelId,
                authorId = authorId,
                content = content,
                imageUrl = imageUrl,
                parentConfessionId = null,
                messageId = 0L
            )

        if (settings.reviewEnabled) {
            val loggingChannelId =
                settings.loggingChannelId
                    ?: return

            val loggingChannel =
                kord.getChannel(
                    Snowflake(loggingChannelId)
                )
                    ?.asChannelOf<MessageChannel>()

            loggingChannel?.createMessage {
                imageUrl?.let { resolvedImageUrl ->
                    val imageBytes =
                        java.net.URL(resolvedImageUrl)
                            .openStream()
                            .readBytes()

                    val spoilerFilename =
                        ImageUtil.spoilerFilename(
                            prefix = "review",
                            id = confession.id,
                            imageUrl = resolvedImageUrl
                        )

                    addFile(
                        name = spoilerFilename,
                        contentProvider = ChannelProvider {
                            io.ktor.utils.io.ByteReadChannel(imageBytes)
                        }
                    )
                }

                embed {
                    val member =
                        kord.getUser(
                            Snowflake(authorId)
                        )?.asMember(Snowflake(guildId))

                    ConfessionEmbedFactory
                        .buildConfessionEmbed(
                            confessionId = confession.id,
                            content = confession.content,
                            authorId = confession.authorId,
                            titleText = "<#$channelId>",
                            embedColor = member?.displayColor()
                        )
                        .invoke(this)

                    member?.let {
                        field {
                            name = "Submitted By"
                            value = it.stableEmojiIdentity()
                            inline = true
                        }
                    }

                    footer {
                        text = "Pending Moderator Review"
                    }
                }

                actionRow {
                    interactionButton(
                        style = dev.kord.common.entity.ButtonStyle.Success,
                        customId = "approve:${confession.id}"
                    ) {
                        label = "Approve"
                    }

                    interactionButton(
                        style = dev.kord.common.entity.ButtonStyle.Danger,
                        customId = "reject:${confession.id}"
                    ) {
                        label = "Reject"
                    }
                }
            }

            responder(
                "Confession #${confession.publicConfessionId} submitted for moderator review."
            )

            return
        }

        val channel =
            kord.getChannel(
                Snowflake(channelId)
            )
                ?.asChannelOf<MessageChannel>()
                ?: return

        val publishedMessage =
            channel.createMessage {
                imageUrl?.let { resolvedImageUrl ->
                    val imageBytes =
                        java.net.URL(resolvedImageUrl)
                            .openStream()
                            .readBytes()

                    val spoilerFilename =
                        ImageUtil.spoilerFilename(
                            prefix = "confession",
                            id = confession.id,
                            imageUrl = resolvedImageUrl
                        )

                    addFile(
                        name = spoilerFilename,
                        contentProvider = ChannelProvider {
                            io.ktor.utils.io.ByteReadChannel(imageBytes)
                        }
                    )
                }

                embed {
                    ConfessionEmbedFactory
                        .buildConfessionEmbed(
                            confessionId = confession.id,
                            content = confession.content,
                            authorId = confession.authorId,
                            embedColor = resolveMemberColor(
                                guildId = confession.guildId,
                                authorId = confession.authorId
                            )
                        )
                        .invoke(this)

                    imageUrl?.let { resolvedImageUrl ->
                        val spoilerFilename =
                            ImageUtil.spoilerFilename(
                                prefix = "confession",
                                id = confession.id,
                                imageUrl = resolvedImageUrl
                            )

                        image = "attachment://$spoilerFilename"
                    }
                }

                actionRow {
                    interactionButton(
                        style = dev.kord.common.entity.ButtonStyle.Primary,
                        customId = "submit-confession"
                    ) {
                        label = "Submit a Confession!"
                    }

                    interactionButton(
                        style = dev.kord.common.entity.ButtonStyle.Secondary,
                        customId = "reply-thread:${confession.messageId}"
                    ) {
                        label = "Reply"
                    }
                }
            }

        publishedMessage.edit {
            actionRow {
                interactionButton(
                    style = dev.kord.common.entity.ButtonStyle.Primary,
                    customId = "submit-confession"
                ) {
                    label = "Submit a Confession!"
                }

                interactionButton(
                    style = dev.kord.common.entity.ButtonStyle.Secondary,
                    customId = "reply-thread:${publishedMessage.id.value}"
                ) {
                    label = "Reply"
                }
            }
        }

        confessionService.updateMessageId(
            confessionId = confession.id,
            messageId = publishedMessage.id.value.toLong()
        )

        responder(
            "Confession #${confession.publicConfessionId} submitted successfully."
        )
    }

    private suspend fun ChatInputCommandInteractionCreateEvent.handleReply() {
        val confessionId =
            interaction.command.integers["confession_id"]
                ?.toInt()
                ?: return

        val reply =
            interaction.command.strings["reply"]
                ?: return

        val guildId =
            interaction.guildId?.value?.toLong()
                ?: return

        val (confession, content) = replyService.buildReply(
            guildId = guildId,
            confessionId = confessionId,
            content = reply
        )

        val thread =
            ensureReplyThread(confession)
                ?: return

        val settings =
            guildConfigService.getSettings(confession.guildId)

        val replyRecord =
            confessionService.createConfession(
                guildId = confession.guildId,
                channelId = thread.id.value.toLong(),
                authorId = interaction.user.id.value.toLong(),
                content = content,
                imageUrl = null,
                parentConfessionId = confession.id,
                messageId = 0L,
            )

        if (settings?.reviewEnabled == true) {
            val loggingChannelId =
                settings.loggingChannelId

            if (loggingChannelId != null) {
                val loggingChannel =
                    kord.getChannel(
                        Snowflake(loggingChannelId)
                    )
                        ?.asChannelOf<MessageChannel>()

                loggingChannel?.createMessage {
                    embed {
                        ConfessionEmbedFactory
                            .buildReplyEmbed(
                                confessionId = confession.id,
                                content = content,
                                authorId = confession.authorId,
                                titleText = "Pending Anonymous Reply (#${confession.id})",
                                embedColor = resolveMemberColor(
                                    guildId = confession.guildId,
                                    authorId = confession.authorId
                                )
                            )
                            .invoke(this)

                        field {
                            name = "Replying To"
                            value =
                                confession.content
                                    .take(1024)
                        }

                        footer {
                            text = "Pending Moderator Review"
                        }
                    }

                    actionRow {
                        interactionButton(
                            style = dev.kord.common.entity.ButtonStyle.Success,
                            customId = "approve-reply:${replyRecord.id}"
                        ) {
                            label = "Approve Reply"
                        }

                        interactionButton(
                            style = dev.kord.common.entity.ButtonStyle.Danger,
                            customId = "reject:${replyRecord.id}"
                        ) {
                            label = "Reject Reply"
                        }
                    }
                }
            }

            interaction.respondEphemeral {
                this.content =
                    "Anonymous reply submitted for moderator review."
            }

            return
        }

        val targetMessageId =
            if (confession.channelId == thread.id.value.toLong()) {
                confession.messageId
            } else {
                null
            }

        val replyMessage = thread.createMessage {
            targetMessageId?.let {
                messageReference = Snowflake(it)
            }

            embed {
                ConfessionEmbedFactory
                    .buildReplyEmbed(
                        confessionId = confessionId,
                        content = content,
                        authorId = confession.authorId,
                        embedColor = resolveMemberColor(
                            guildId = confession.guildId,
                            authorId = confession.authorId
                        )
                    )
                    .invoke(this)
            }
        }

        replyMessage.edit {
            actionRow {
                interactionButton(
                    style = dev.kord.common.entity.ButtonStyle.Secondary,
                    customId = "reply-thread:${replyMessage.id.value}"
                ) {
                    label = "Reply"
                }
            }
        }

        confessionService.updateMessageId(
            confessionId = replyRecord.id,
            messageId = replyMessage.id.value.toLong()
        )

        interaction.respondEphemeral {
            this.content =
                "Anonymous reply delivered to confession #${confession.publicConfessionId}."
        }
    }


    private suspend fun ChatInputCommandInteractionCreateEvent.handleReport() {
        val confessionId =
            interaction.command.integers["confession_id"]
                ?: return

        interaction.respondEphemeral {
            content =
                "Confession #$confessionId has been flagged for moderator review."
        }
    }

    private suspend fun ChatInputCommandInteractionCreateEvent.handleConfig() {
        val guildInteraction =
            interaction as? GuildInteraction
                ?: return

        if (!requireAdmin()) {
            return
        }

        val guildId =
            guildInteraction.guildId.value.toLong()
        val subCommand =
            (interaction.command as? SubCommand)?.name

        when (subCommand) {
            "channel" -> {
                guildConfigService.configureChannel(
                    guildId = guildId,
                    channelId = interaction.channelId.value.toLong()
                )

                interaction.respondEphemeral {
                    content =
                        "Current channel configured as the confession channel."
                }
            }

            "review" -> {
                val current =
                    guildConfigService.getSettings(guildId)

                val enabled =
                    !(current?.reviewEnabled ?: false)

                guildConfigService.setReviewMode(
                    guildId = guildId,
                    enabled = enabled
                )

                interaction.respondEphemeral {
                    content =
                        "Review mode is now ${if (enabled) "enabled" else "disabled"}."
                }
            }

            "logging" -> {
                guildConfigService.configureLogging(
                    guildId = guildId,
                    channelId = interaction.channelId.value.toLong()
                )

                interaction.respondEphemeral {
                    content =
                        "Current channel configured as the moderation log channel."
                }
            }

            "addmodrole" -> {
                val roleId =
                    interaction.command.roles["role"]
                        ?.id
                        ?.value
                        ?.toLong()
                        ?: return

                guildConfigService.addModeratorRole(
                    guildId = guildId,
                    roleId = roleId
                )

                interaction.respondEphemeral {
                    content =
                        "Role <@&$roleId> can now administer the bot."
                }
            }

            "removemodrole" -> {
                val roleId =
                    interaction.command.roles["role"]
                        ?.id
                        ?.value
                        ?.toLong()
                        ?: return

                guildConfigService.removeModeratorRole(
                    guildId = guildId,
                    roleId = roleId
                )

                interaction.respondEphemeral {
                    content =
                        "Role <@&$roleId> no longer has bot administration access."
                }
            }

            "listmodroles" -> {
                val roles =
                    guildConfigService.getModeratorRoles(guildId)

                interaction.respondEphemeral {
                    content =
                        if (roles.isEmpty()) {
                            "No moderator roles configured."
                        } else {
                            roles.joinToString(
                                prefix = "Authorized moderator roles:\n",
                                separator = "\n"
                            ) {
                                "<@&$it>"
                            }
                        }
                }
            }

            else -> {
                interaction.respondEphemeral {
                    content = "Unknown configuration command."
                }
            }
        }
    }


    private suspend fun ensureReplyThread(
        confession: dev.shalaga44.models.Confession
    ): GuildMessageChannel? {
        val rawChannel = kord.getChannel(
            Snowflake(confession.channelId)
        )
            ?: return null

        val existingThreadChannel =
            rawChannel.asChannelOrNull() as? GuildMessageChannel

        if (existingThreadChannel != null &&
            rawChannel !is TextChannel
        ) {
            return existingThreadChannel
        }

        val channel =
            rawChannel.asChannelOf<TextChannel>()

        val message = try {
            channel.getMessage(
                Snowflake(confession.messageId)
            )
        } catch (_: dev.kord.core.exception.EntityNotFoundException) {
            return findExistingReplyThread(confession)
        }

        val existingThread =
            message.fetchPublicThreadOrNull()

        if (existingThread != null) {
            return existingThread
        }

        return try {
            channel.startPublicThreadWithMessage(
                messageId = message.id,
                name = "confession-${confession.publicConfessionId}-replies"
            )
        } catch (_: Exception) {
            message.fetchPublicThreadOrNull()
                ?: findExistingReplyThread(confession)
        }
    }

    private suspend fun findExistingReplyThread(
        confession: dev.shalaga44.models.Confession
    ): GuildMessageChannel? {
        val threadChannel =
            kord.getChannel(
                Snowflake(confession.channelId)
            )
                ?.asChannelOrNull() as? GuildMessageChannel

        if (threadChannel != null &&
            threadChannel.name.startsWith("confession-${confession.publicConfessionId}-replies")
        ) {
            return threadChannel
        }

        return null
    }

    private suspend fun Message.fetchPublicThreadOrNull(): GuildMessageChannel? {
        val threadId =
            data.thread?.value?.id
                ?: return null

        return kord.getChannel(threadId)
            ?.asChannelOrNull() as? GuildMessageChannel
    }

    private suspend fun ChatInputCommandInteractionCreateEvent.handleHelp() {
        val section =
            interaction.command.strings["section"]

        interaction.respondEphemeral {
            embed {
                when (section) {
                    "admin" -> {
                        HelpEmbedFactory
                            .buildAdminHelpEmbed()
                            .invoke(this)
                    }

                    else -> {
                        HelpEmbedFactory
                            .buildUserHelpEmbed()
                            .invoke(this)
                    }
                }
            }
        }
    }
}