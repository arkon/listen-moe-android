package me.echeung.moemoekyun.api.callbacks;

import me.echeung.moemoekyun.api.models.Artist;

public interface ArtistCallback extends BaseCallback {
    void onSuccess(final Artist artist);
}