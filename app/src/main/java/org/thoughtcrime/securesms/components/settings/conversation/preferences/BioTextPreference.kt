package org.thoughtcrime.securesms.components.settings.conversation.preferences

import android.content.ClipData
import android.content.Context
import android.view.View
import android.widget.TextView
import android.widget.Toast
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.components.settings.PreferenceModel
import org.thoughtcrime.securesms.phonenumbers.PhoneNumberFormatter
import org.thoughtcrime.securesms.recipients.Recipient
import org.thoughtcrime.securesms.util.ServiceUtil
import org.thoughtcrime.securesms.util.adapter.mapping.LayoutFactory
import org.thoughtcrime.securesms.util.adapter.mapping.MappingAdapter
import org.thoughtcrime.securesms.util.adapter.mapping.MappingViewHolder

/**
 * Renders name, description, about, etc. for a given group or recipient.
 */
object BioTextPreference {

  fun register(adapter: MappingAdapter) {
    adapter.registerFactory(RecipientModel::class.java, LayoutFactory(::RecipientViewHolder, R.layout.conversation_settings_bio_preference_item))
    adapter.registerFactory(GroupModel::class.java, LayoutFactory(::GroupViewHolder, R.layout.conversation_settings_bio_preference_item))
  }

  abstract class BioTextPreferenceModel<T : BioTextPreferenceModel<T>> : PreferenceModel<T>() {
    abstract fun getHeadlineText(context: Context): String
    abstract fun getSubhead1Text(context: Context): String?
    abstract fun getSubhead2Text(): String?
    abstract fun getCompoundDrawable(): Int
  }

  class RecipientModel(
    private val recipient: Recipient,
  ) : BioTextPreferenceModel<RecipientModel>() {

    override fun getHeadlineText(context: Context): String = recipient.getDisplayNameOrUsername(context)

    override fun getSubhead1Text(context: Context): String? {
      return if (recipient.isReleaseNotes) {
        context.getString(R.string.ReleaseNotes__signal_release_notes_and_news)
      } else {
        recipient.combinedAboutAndEmoji
      }
    }

    override fun getSubhead2Text(): String? = recipient.e164.transform(PhoneNumberFormatter::prettyPrint).orNull()

    override fun getCompoundDrawable(): Int {
      return if (recipient.isReleaseNotes) R.drawable.ic_official_28 else 0
    }

    override fun areContentsTheSame(newItem: RecipientModel): Boolean {
      return super.areContentsTheSame(newItem) && newItem.recipient.hasSameContent(recipient)
    }

    override fun areItemsTheSame(newItem: RecipientModel): Boolean {
      return newItem.recipient.id == recipient.id
    }
  }

  class GroupModel(
    val groupTitle: String,
    val groupMembershipDescription: String?
  ) : BioTextPreferenceModel<GroupModel>() {
    override fun getHeadlineText(context: Context): String = groupTitle

    override fun getSubhead1Text(context: Context): String? = groupMembershipDescription

    override fun getSubhead2Text(): String? = null

    override fun getCompoundDrawable(): Int = 0

    override fun areContentsTheSame(newItem: GroupModel): Boolean {
      return super.areContentsTheSame(newItem) &&
        groupTitle == newItem.groupTitle &&
        groupMembershipDescription == newItem.groupMembershipDescription
    }

    override fun areItemsTheSame(newItem: GroupModel): Boolean {
      return true
    }
  }

  private abstract class BioTextViewHolder<T : BioTextPreferenceModel<T>>(itemView: View) : MappingViewHolder<T>(itemView) {

    private val headline: TextView = itemView.findViewById(R.id.bio_preference_headline)
    private val subhead1: TextView = itemView.findViewById(R.id.bio_preference_subhead_1)
    protected val subhead2: TextView = itemView.findViewById(R.id.bio_preference_subhead_2)

    override fun bind(model: T) {
      headline.text = model.getHeadlineText(context)
      headline.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, model.getCompoundDrawable(), 0)

      model.getSubhead1Text(context).let {
        subhead1.text = it
        subhead1.visibility = if (it == null) View.GONE else View.VISIBLE
      }

      model.getSubhead2Text().let {
        subhead2.text = it
        subhead2.visibility = if (it == null) View.GONE else View.VISIBLE
      }
    }
  }

  private class RecipientViewHolder(itemView: View) : BioTextViewHolder<RecipientModel>(itemView) {
    override fun bind(model: RecipientModel) {
      super.bind(model)

      val phoneNumber = model.getSubhead2Text()
      if (!phoneNumber.isNullOrEmpty()) {
        subhead2.setOnLongClickListener {
          val clipboardManager = ServiceUtil.getClipboardManager(context)
          clipboardManager.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.ConversationSettingsFragment__phone_number), subhead2.text.toString()))
          Toast.makeText(context, R.string.ConversationSettingsFragment__copied_phone_number_to_clipboard, Toast.LENGTH_SHORT).show()
          true
        }
      } else {
        subhead2.setOnLongClickListener(null)
      }
    }
  }

  private class GroupViewHolder(itemView: View) : BioTextViewHolder<GroupModel>(itemView)
}
