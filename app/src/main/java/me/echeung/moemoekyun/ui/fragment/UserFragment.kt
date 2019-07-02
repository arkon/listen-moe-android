package me.echeung.moemoekyun.ui.fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import me.echeung.moemoekyun.App
import me.echeung.moemoekyun.R
import me.echeung.moemoekyun.adapter.SongsAdapter
import me.echeung.moemoekyun.client.api.callback.UserFavoritesCallback
import me.echeung.moemoekyun.client.api.callback.UserInfoCallback
import me.echeung.moemoekyun.client.api.library.Library
import me.echeung.moemoekyun.client.model.Song
import me.echeung.moemoekyun.client.model.User
import me.echeung.moemoekyun.databinding.FragmentUserBinding
import me.echeung.moemoekyun.ui.activity.MainActivity
import me.echeung.moemoekyun.ui.base.SongsListBaseFragment
import me.echeung.moemoekyun.ui.view.SongList
import me.echeung.moemoekyun.util.SongActionsUtil
import me.echeung.moemoekyun.viewmodel.UserViewModel

class UserFragment : SongsListBaseFragment<FragmentUserBinding>(), SongList.SongListLoader, SharedPreferences.OnSharedPreferenceChangeListener {

    private val userVm: UserViewModel = App.userViewModel!!

    init {
        layout = R.layout.fragment_user

        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action
                if (action != null) {
                    when (action) {
                        MainActivity.AUTH_EVENT, SongActionsUtil.FAVORITE_EVENT -> initUserContent()
                    }
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        binding.radioVm = App.radioViewModel
        binding.userVm = userVm
        binding.songListVm = songListVm

        initUserContent()

        return view
    }

    override fun initSongList(binding: FragmentUserBinding): SongList {
        return SongList(
                requireActivity(),
                songListVm,
                binding.favorites.favoritesList.list,
                binding.favorites.favoritesList.refreshLayout,
                binding.favorites.filter.query,
                binding.favorites.filter.overflowBtn,
                LIST_ID,
                this)
    }

    override fun loadSongs(adapter: SongsAdapter) {
        App.radioClient!!.api.getUserFavorites(object : UserFavoritesCallback {
            override fun onSuccess(favorites: List<Song>) {
                activity?.runOnUiThread {
                    songList.showLoading(false)
                    adapter.songs = favorites
                    userVm.hasFavorites = favorites.isNotEmpty()
                }
            }

            override fun onFailure(message: String?) {
                activity?.runOnUiThread { songList.showLoading(false) }
            }
        })
    }

    private fun initUserContent() {
        songList.showLoading(true)

        if (App.authUtil.isAuthenticated) {
            getUserInfo()
            songList.loadSongs()
        }
    }

    private fun getUserInfo() {
        App.radioClient!!.api.getUserInfo(object : UserInfoCallback {
            override fun onSuccess(user: User) {
                userVm.user = user

                if (user.avatarImage != null) {
                    userVm.avatarUrl = Library.CDN_AVATAR_URL + user.avatarImage
                }

                if (user.bannerImage != null) {
                    userVm.bannerUrl = Library.CDN_BANNER_URL + user.bannerImage
                }
            }

            override fun onFailure(message: String?) {}
        })
    }

    companion object {
        private const val LIST_ID = "FAVORITES_LIST"
    }
}
