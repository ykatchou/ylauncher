package com.ylauncher.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ylauncher.data.model.FavoriteApp
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorite_apps ORDER BY position ASC")
    fun getAllFavorites(): Flow<List<FavoriteApp>>

    @Query("SELECT * FROM favorite_apps ORDER BY position ASC")
    suspend fun getAllFavoritesOnce(): List<FavoriteApp>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteApp)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(favorites: List<FavoriteApp>)

    @Update
    suspend fun updateFavorite(favorite: FavoriteApp)

    @Query("DELETE FROM favorite_apps WHERE position = :position")
    suspend fun deleteFavoriteAt(position: Int)

    @Query("DELETE FROM favorite_apps")
    suspend fun deleteAll()

    @Query("DELETE FROM favorite_apps WHERE panelId = :panelId")
    suspend fun deleteByPanel(panelId: Int)

    @Query("SELECT COUNT(*) FROM favorite_apps")
    suspend fun count(): Int
}
