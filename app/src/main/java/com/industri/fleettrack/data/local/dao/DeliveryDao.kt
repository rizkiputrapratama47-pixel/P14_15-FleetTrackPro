package com.industri.fleettrack.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.industri.fleettrack.data.local.entity.DeliveryOrderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeliveryDao {

    // Mengamati seluruh daftar tugas pengiriman secara reaktif
    @Query("SELECT * FROM delivery_orders ORDER BY updated_at DESC")
    fun getAllDeliveryOrders(): Flow<List<DeliveryOrderEntity>>

    // Menyaring tugas berdasarkan status pengiriman
    @Query(
        "SELECT * FROM delivery_orders WHERE delivery_status = :status"
    )
    fun getOrdersByStatus(
        status: String
    ): Flow<List<DeliveryOrderEntity>>

    // Menyimpan atau memperbarui data hasil sinkronisasi
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(
        orders: List<DeliveryOrderEntity>
    )

    // Memperbarui status pengiriman
    @Query(
        """
        UPDATE delivery_orders
        SET delivery_status = :status,
            is_synced = :isSynced,
            updated_at = :timestamp
        WHERE order_id = :orderId
        """
    )
    suspend fun updateOrderStatus(
        orderId: String,
        status: String,
        isSynced: Boolean,
        timestamp: Long
    )

    // Menghapus seluruh data pengiriman
    @Query("DELETE FROM delivery_orders")
    suspend fun clearAll()
}
