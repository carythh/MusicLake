package com.cyl.musiclake.data

import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import android.text.TextUtils
import com.cyl.musiclake.bean.Album
import com.cyl.musiclake.bean.Artist
import com.cyl.musiclake.bean.SongInfo
import com.cyl.musiclake.common.Constants
import com.cyl.musiclake.data.db.DaoLitepal
import com.cyl.musiclake.utils.CoverLoader
import com.cyl.musiclake.utils.LogUtil
import com.music.lake.musiclib.bean.BaseMusicInfo
import org.litepal.LitePal


object SongLoader {
    /**
     * 获取所有艺术家
     *
     * @param context
     * @return
     */
    fun getAllArtists(): MutableList<Artist> {
        return DaoLitepal.updateArtistList()
    }

    /**
     * 获取艺术家所有歌曲
     *
     * @param context
     * @return
     */
    fun getSongsForArtist(artistName: String?): MutableList<BaseMusicInfo> {
        val data = LitePal.where("isonline =0 and artist like ?", "%$artistName%").find(SongInfo::class.java)
        val musicList = mutableListOf<BaseMusicInfo>()
        data.forEach {
            musicList.add(it.convertToMusicInfo())
        }
        return musicList;
    }

    /**
     * 获取专辑所有歌曲
     *
     * @param context
     * @return
     */
    fun getSongsForAlbum(albumName: String?): MutableList<BaseMusicInfo> {
        val data = LitePal.where("isonline =0 and album like ?", "%$albumName%").find(SongInfo::class.java)
        val musicList = mutableListOf<BaseMusicInfo>()
        data.forEach {
            musicList.add(it.convertToMusicInfo())
        }
        return musicList;
    }

    /**
     * 获取所有专辑
     *
     * @param context
     * @return
     */
    fun getAllAlbums(): MutableList<Album> {
        return DaoLitepal.updateAlbumList()
    }

    /**
     * 获取所有专辑
     *
     * @param context
     * @return
     */
    fun getAllAlbums(artistName: String?): MutableList<Album> {
        return LitePal.where("type =? and artistName like ?", "local", "%$artistName%").find(Album::class.java)
    }


    /**
     * Android 扫描获取到的数据
     *
     * @param context
     * @param cursor
     * @return
     */
    private fun getSongsForMedia(context: Context, cursor: Cursor?): MutableList<BaseMusicInfo> {
        val results = mutableListOf<BaseMusicInfo>()
        try {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    val id = cursor.getLong(0)
                    val title = cursor.getString(1)
                    val artist = cursor.getString(2)
                    val album = cursor.getString(3)
                    val duration = cursor.getInt(4)
                    val trackNumber = cursor.getInt(5)
                    val artistId = cursor.getString(6)
                    val albumId = cursor.getString(7)
                    val path = cursor.getString(8)
                    val coverUri = CoverLoader.getCoverUri(context, albumId)
                    val music = BaseMusicInfo()
                    music.type = Constants.LOCAL
                    music.isOnline = false
                    music.mid = id.toString()
                    music.album = album
                    music.albumId = albumId
                    music.artist = if (artist == "<unknown>") "未知歌手" else artist
                    music.artistId = artistId
                    music.uri = path
                    coverUri?.let { music.coverUri = it }
                    music.trackNumber = trackNumber
                    music.duration = duration.toLong()
                    music.title = title
                    music.date = System.currentTimeMillis()
                    DaoLitepal.saveOrUpdateMusic(music)
                    results.add(music)
                } while (cursor.moveToNext())
            }
            cursor?.close()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        return results
    }


    /**
     * 获取所有收藏的歌曲
     *
     * @param context
     * @return
     */
    fun getFavoriteSong(): MutableList<BaseMusicInfo> {
        return DaoLitepal.getMusicList(Constants.PLAYLIST_LOVE_ID)
    }

    /**
     * 获取本地歌曲
     */
    private fun getSongsForDB(): MutableList<BaseMusicInfo> {
        return DaoLitepal.getMusicList(Constants.PLAYLIST_LOCAL_ID)
    }

    fun getLocalMusic(context: Context, isReload: Boolean = true): MutableList<BaseMusicInfo> {
        LogUtil.d("SongLoader", "getLocalMusic =$isReload")
        val data = getSongsForDB()
        if (data.size == 0 || isReload) {
            data.clear()
            val musicLists = getAllLocalSongs(context)
//            if (isReload) {
//                DaoLitepal.updateAlbumList()
//                DaoLitepal.updateArtistList()
//            }
            musicLists.forEach {
                data.add(it)
            }
        }
        return data
    }

    fun getMusicInfo(mid: String): BaseMusicInfo? {
        return DaoLitepal.getMusicInfo(mid)
    }


    fun updateFavoriteSong(baseMusicInfoInfo: BaseMusicInfo): Boolean {
        baseMusicInfoInfo.isLove = !baseMusicInfoInfo.isLove
        DaoLitepal.saveOrUpdateMusic(baseMusicInfoInfo)
        return baseMusicInfoInfo.isLove
    }

    /**
     * 本地歌曲
     * 添加歌曲
     */
    private fun insertSongs(baseMusicInfos: List<BaseMusicInfo>) {
    }

    /**
     * 本地歌曲
     * 添加歌曲
     */
    fun updateMusic(baseMusicInfoInfo: BaseMusicInfo) {
        DaoLitepal.saveOrUpdateMusic(baseMusicInfoInfo)
    }

    /**
     * 删除歌曲
     */
    fun removeSong(music: BaseMusicInfo) {
        DaoLitepal.deleteMusic(music)
    }

    fun removeMusicList(baseMusicInfoInfoList: MutableList<BaseMusicInfo>) {
        baseMusicInfoInfoList.forEach {
            removeSong(it)
        }
    }

    fun getAllLocalSongs(context: Context): MutableList<BaseMusicInfo> {
        return getSongsForMedia(context, makeSongCursor(context, null, null))
    }

    fun searchSongs(context: Context, searchString: String): MutableList<BaseMusicInfo> {
        return getSongsForMedia(context, makeSongCursor(context, "title LIKE ? or artist LIKE ? or album LIKE ? ",
                arrayOf("%$searchString%", "%$searchString%", "%$searchString%")))
    }

    fun getSongListInFolder(context: Context, path: String): MutableList<BaseMusicInfo> {
        val whereArgs = arrayOf("$path%")
        return getSongsForMedia(context, makeSongCursor(context, MediaStore.Audio.Media.DATA + " LIKE ?", whereArgs, null))
    }

    fun makeSongCursor(context: Context, selection: String?, paramArrayOfString: Array<String>?): Cursor? {
        val songSortOrder = MediaStore.Audio.Media.DEFAULT_SORT_ORDER
        return makeSongCursor(context, selection, paramArrayOfString, songSortOrder)
    }

    /**
     * 搜素本地音乐
     */
    private fun makeSongCursor(context: Context, selection: String?, paramArrayOfString: Array<String>?, sortOrder: String?): Cursor? {
        var selectionStatement = "duration>60000 AND is_music=1 AND title != ''"

        if (!TextUtils.isEmpty(selection)) {
            selectionStatement = "$selectionStatement AND $selection"
        }
        return context.contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                arrayOf("_id", "title", "artist", "album", "duration", "track", "artist_id", "album_id", MediaStore.Audio.Media.DATA, "is_music"),
                selectionStatement, paramArrayOfString, sortOrder)
    }


}
