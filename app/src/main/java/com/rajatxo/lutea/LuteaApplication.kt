package com.rajatxo.lutea

import android.app.Application
import com.rajatxo.lutea.audio.TypewriterSound
import com.rajatxo.lutea.data.prefs.PlaylistStyleManager
import com.rajatxo.lutea.data.prefs.UserNameManager
import com.rajatxo.lutea.data.store.FavouriteStore
import com.rajatxo.lutea.data.store.PlaylistStore
import com.rajatxo.lutea.data.store.SongEditStore

class LuteaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PlaylistStore.init(this)
        FavouriteStore.init(this)
        SongEditStore.init(this)
        PlaylistStyleManager.init(this)
        UserNameManager.init(this)
        TypewriterSound.init(this)
    }
}
