package com.miaudioplay.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.miaudioplay.data.models.PlaylistSong;
import java.lang.Boolean;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class PlaylistSongDao_Impl implements PlaylistSongDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<PlaylistSong> __insertionAdapterOfPlaylistSong;

  private final EntityDeletionOrUpdateAdapter<PlaylistSong> __deletionAdapterOfPlaylistSong;

  private final SharedSQLiteStatement __preparedStmtOfRemoveSongFromPlaylist;

  public PlaylistSongDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfPlaylistSong = new EntityInsertionAdapter<PlaylistSong>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `playlist_songs` (`id`,`playlistId`,`songId`,`songUri`,`addedAt`) VALUES (nullif(?, 0),?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PlaylistSong entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getPlaylistId());
        statement.bindLong(3, entity.getSongId());
        statement.bindString(4, entity.getSongUri());
        statement.bindLong(5, entity.getAddedAt());
      }
    };
    this.__deletionAdapterOfPlaylistSong = new EntityDeletionOrUpdateAdapter<PlaylistSong>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `playlist_songs` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PlaylistSong entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__preparedStmtOfRemoveSongFromPlaylist = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM playlist_songs WHERE playlistId = ? AND songId = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertPlaylistSong(final PlaylistSong playlistSong,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfPlaylistSong.insert(playlistSong);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deletePlaylistSong(final PlaylistSong playlistSong,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfPlaylistSong.handle(playlistSong);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object removeSongFromPlaylist(final long playlistId, final long songId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfRemoveSongFromPlaylist.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, playlistId);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, songId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfRemoveSongFromPlaylist.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<PlaylistSong>> getSongsForPlaylist(final long playlistId) {
    final String _sql = "SELECT * FROM playlist_songs WHERE playlistId = ? ORDER BY addedAt";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, playlistId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"playlist_songs"}, new Callable<List<PlaylistSong>>() {
      @Override
      @NonNull
      public List<PlaylistSong> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPlaylistId = CursorUtil.getColumnIndexOrThrow(_cursor, "playlistId");
          final int _cursorIndexOfSongId = CursorUtil.getColumnIndexOrThrow(_cursor, "songId");
          final int _cursorIndexOfSongUri = CursorUtil.getColumnIndexOrThrow(_cursor, "songUri");
          final int _cursorIndexOfAddedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "addedAt");
          final List<PlaylistSong> _result = new ArrayList<PlaylistSong>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PlaylistSong _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpPlaylistId;
            _tmpPlaylistId = _cursor.getLong(_cursorIndexOfPlaylistId);
            final long _tmpSongId;
            _tmpSongId = _cursor.getLong(_cursorIndexOfSongId);
            final String _tmpSongUri;
            _tmpSongUri = _cursor.getString(_cursorIndexOfSongUri);
            final long _tmpAddedAt;
            _tmpAddedAt = _cursor.getLong(_cursorIndexOfAddedAt);
            _item = new PlaylistSong(_tmpId,_tmpPlaylistId,_tmpSongId,_tmpSongUri,_tmpAddedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getSongUrisForPlaylist(final long playlistId,
      final Continuation<? super List<String>> $completion) {
    final String _sql = "SELECT songUri FROM playlist_songs WHERE playlistId = ? ORDER BY addedAt";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, playlistId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<String>>() {
      @Override
      @NonNull
      public List<String> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final List<String> _result = new ArrayList<String>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final String _item;
            _item = _cursor.getString(0);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object isSongInPlaylist(final long playlistId, final long songId,
      final Continuation<? super Boolean> $completion) {
    final String _sql = "SELECT EXISTS(SELECT 1 FROM playlist_songs WHERE playlistId = ? AND songId = ?)";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, playlistId);
    _argIndex = 2;
    _statement.bindLong(_argIndex, songId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Boolean>() {
      @Override
      @NonNull
      public Boolean call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Boolean _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp != 0;
          } else {
            _result = false;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getSongCount(final long playlistId,
      final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM playlist_songs WHERE playlistId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, playlistId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
