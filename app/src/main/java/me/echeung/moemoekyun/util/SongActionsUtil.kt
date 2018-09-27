package me.echeung.moemoekyun.util

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import me.echeung.moemoekyun.App
import me.echeung.moemoekyun.R
import me.echeung.moemoekyun.adapter.SongDetailAdapter
import me.echeung.moemoekyun.client.api.callback.FavoriteSongCallback
import me.echeung.moemoekyun.client.api.callback.RequestSongCallback
import me.echeung.moemoekyun.client.model.Song
import java.util.*

object SongActionsUtil {

    const val REQUEST_EVENT = "req_event"
    const val FAVORITE_EVENT = "fav_event"

    @JvmStatic
    fun showSongsDialog(activity: Activity, title: String, song: Song) {
        val songList = ArrayList<Song>()
        songList.add(song)

        showSongsDialog(activity, title, songList)
    }

    @JvmStatic
    fun showSongsDialog(activity: Activity?, title: String, songs: List<Song>) {
        if (activity == null) return

        AlertDialog.Builder(activity, R.style.DialogTheme)
                .setTitle(title)
                .setAdapter(SongDetailAdapter(activity, songs), null)
                .setPositiveButton(R.string.close, null)
                .create()
                .show()
    }

    /**
     * Updates the favorite status of a song.
     *
     * @param song The song to update the favorite status of.
     */
    @JvmStatic
    fun toggleFavorite(activity: Activity?, song: Song) {
        val songId = song.id
        val isCurrentlyFavorite = song.isFavorite

        val callback = object : FavoriteSongCallback {
            override fun onSuccess() {
                if (App.radioViewModel!!.currentSong!!.id == songId) {
                    App.radioViewModel!!.isFavorited = !isCurrentlyFavorite
                }
                song.isFavorite = !isCurrentlyFavorite

                if (activity == null) return

                activity.runOnUiThread {
                    // Broadcast event
                    activity.sendBroadcast(Intent(SongActionsUtil.FAVORITE_EVENT))

                    if (isCurrentlyFavorite) {
                        // Undo action
                        val coordinatorLayout = activity.findViewById<View>(R.id.coordinator_layout)
                        if (coordinatorLayout != null) {
                            val undoBar = Snackbar.make(coordinatorLayout,
                                    String.format(activity.getString(R.string.unfavorited), song.toString()),
                                    Snackbar.LENGTH_LONG)
                            undoBar.setAction(R.string.action_undo) { v -> toggleFavorite(activity, song) }
                            undoBar.show()
                        }
                    }
                }
            }

            override fun onFailure(message: String) {
                if (activity == null) return

                activity.runOnUiThread { Toast.makeText(activity.applicationContext, message, Toast.LENGTH_SHORT).show() }
            }
        }

        App.radioClient!!.api.toggleFavorite(songId.toString(), isCurrentlyFavorite, callback)
    }

    /**
     * Requests a song.
     *
     * @param song The song to request.
     */
    @JvmStatic
    fun request(activity: Activity?, song: Song) {
        val user = App.userViewModel!!.user ?: return

        App.radioClient!!.api.requestSong(song.id.toString(), object : RequestSongCallback {
            override fun onSuccess() {
                if (activity == null) return

                activity.runOnUiThread {
                    // Broadcast event
                    activity.sendBroadcast(Intent(SongActionsUtil.REQUEST_EVENT))

                    // Instantly update remaining requests number to appear responsive
                    val remainingRequests = user.requestsRemaining - 1
                    App.userViewModel!!.requestsRemaining = remainingRequests

                    val toastMsg = if (App.preferenceUtil!!.shouldShowRandomRequestTitle())
                        activity.getString(R.string.requested_song, song.toString())
                    else
                        activity.getString(R.string.requested_random_song)

                    Toast.makeText(activity.applicationContext, toastMsg, Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(message: String) {
                if (activity == null) return

                activity.runOnUiThread { Toast.makeText(activity.applicationContext, message, Toast.LENGTH_SHORT).show() }
            }
        })
    }

    @JvmStatic
    fun copyToClipboard(context: Context?, song: Song?) {
        if (context == null || song == null) return

        copyToClipboard(context, song.toString())
    }

    @JvmStatic
    fun copyToClipboard(context: Context, songInfo: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("song", songInfo)
        clipboard.primaryClip = clip

        val text = String.format("%s: %s", context.getString(R.string.copied_to_clipboard), songInfo)

        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }

}