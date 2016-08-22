package com.hiroshi.cimoc.core.manager;

import android.database.Cursor;

import com.hiroshi.cimoc.CimocApplication;
import com.hiroshi.cimoc.model.Comic;
import com.hiroshi.cimoc.model.ComicDao;
import com.hiroshi.cimoc.model.ComicDao.Properties;
import com.hiroshi.cimoc.model.MiniComic;
import com.hiroshi.cimoc.rx.RxBus;
import com.hiroshi.cimoc.rx.RxEvent;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Hiroshi on 2016/7/9.
 */
public class ComicManager {

    public static final long NEW_VALUE = 0xFFFFFFFFFFFL;

    private static ComicManager mComicManager;

    private ComicDao mComicDao;

    private ComicManager() {
        mComicDao = CimocApplication.getDaoSession().getComicDao();
    }

    public void restoreFavorite(final List<Comic> list) {
        mComicDao.getSession().runInTx(new Runnable() {
            @Override
            public void run() {
                List<MiniComic> result = new LinkedList<>();
                for (Comic comic : list) {
                    Comic temp = mComicDao.queryBuilder()
                            .where(Properties.Source.eq(comic.getSource()), Properties.Cid.eq(comic.getCid()))
                            .unique();
                    if (temp == null) {
                        comic.setFavorite(System.currentTimeMillis());
                        long id = mComicDao.insert(comic);
                        comic.setId(id);
                        result.add(0, new MiniComic(comic));
                    } else if (temp.getFavorite() == null) {
                        temp.setFavorite(System.currentTimeMillis());
                        mComicDao.update(temp);
                        result.add(0, new MiniComic(temp));
                    }
                }
                RxBus.getInstance().post(new RxEvent(RxEvent.RESTORE_FAVORITE, result));
            }
        });
    }

    public void updateFavorite(final List<MiniComic> list) {
        mComicDao.getSession().runInTx(new Runnable() {
            @Override
            public void run() {
                for (MiniComic comic : list) {
                    Comic temp = mComicDao.load(comic.getId());
                    if (temp != null && !comic.getUpdate().equals(temp.getUpdate())) {
                        temp.setUpdate(comic.getUpdate());
                        temp.setFavorite(NEW_VALUE);
                        mComicDao.update(temp);
                    }
                }
            }
        });
    }

    public void deleteBySource(final int source) {
        mComicDao.getSession().runInTx(new Runnable() {
            @Override
            public void run() {
                List<Comic> list = mComicDao.queryBuilder()
                        .where(Properties.Source.eq(source))
                        .list();
                for (Comic comic : list) {
                    mComicDao.delete(comic);
                }
            }
        });
    }

    public void deleteFavorite(long id) {
        Comic comic = mComicDao.load(id);
        if (comic.getHistory() == null) {
            mComicDao.delete(comic);
        } else {
            comic.setFavorite(null);
            mComicDao.update(comic);
        }
    }

    public void deleteHistory(long id) {
        Comic comic = mComicDao.load(id);
        if (comic.getFavorite() == null) {
            mComicDao.delete(comic);
        } else {
            comic.setHistory(null);
            mComicDao.update(comic);
        }
    }

    public void cleanHistory() {
        mComicDao.getSession().runInTx(new Runnable() {
            @Override
            public void run() {
                List<Comic> list = mComicDao.queryBuilder().where(Properties.History.isNotNull()).list();
                for (Comic comic : list) {
                    if (comic.getFavorite() != null) {
                        comic.setHistory(null);
                        mComicDao.update(comic);
                    } else {
                        mComicDao.delete(comic);
                    }
                }
                RxBus.getInstance().post(new RxEvent(RxEvent.DELETE_HISTORY, list.size()));
            }
        });
    }

    public List<Comic> listBackup() {
        return mComicDao.queryBuilder().where(Properties.Favorite.isNotNull()).list();
    }

    public List<MiniComic> listFavorite() {
        Cursor cursor = mComicDao.queryBuilder()
                .where(ComicDao.Properties.Favorite.isNotNull())
                .orderDesc(Properties.Favorite)
                .buildCursor()
                .query();
        return listByCursor(cursor);
    }

    public List<MiniComic> listHistory() {
        Cursor cursor = mComicDao.queryBuilder()
                .where(Properties.History.isNotNull())
                .orderDesc(Properties.History)
                .buildCursor()
                .query();
        return listByCursor(cursor);
    }

    private List<MiniComic> listByCursor(Cursor cursor) {
        List<MiniComic> list = new LinkedList<>();
        while (cursor.moveToNext()) {
            long id = cursor.getLong(0);
            int source = cursor.getInt(1);
            String cid = cursor.getString(2);
            String title = cursor.getString(3);
            String cover = cursor.getString(4);
            String update = cursor.getString(5);
            boolean status = cursor.getLong(6) == NEW_VALUE;
            list.add(new MiniComic(id, source, cid, title, cover, update, status));
        }
        cursor.close();
        return list;
    }

    public Comic getComic(Long id, int source, String cid) {
        Comic comic;
        if (id == null) {
            comic = mComicDao.queryBuilder()
                    .where(Properties.Source.eq(source), Properties.Cid.eq(cid))
                    .unique();
        } else {
            comic = mComicDao.load(id);
            if (comic.getFavorite() != null && comic.getFavorite() == NEW_VALUE) {
                comic.setFavorite(System.currentTimeMillis());
            }
        }
        if (comic == null) {
            comic = new Comic(source, cid);
        }
        return comic;
    }

    public void updateComic(Comic comic) {
        mComicDao.update(comic);
    }

    public void deleteComic(long id) {
        mComicDao.deleteByKey(id);
    }

    public long insertComic(Comic comic) {
        return mComicDao.insert(comic);
    }

    public static ComicManager getInstance() {
        if (mComicManager == null) {
            mComicManager = new ComicManager();
        }
        return mComicManager;
    }

}