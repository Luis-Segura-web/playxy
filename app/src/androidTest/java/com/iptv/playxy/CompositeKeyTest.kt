package com.iptv.playxy.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.iptv.playxy.domain.LiveStream
import com.iptv.playxy.util.EntityMapper
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pruebas para verificar el correcto funcionamiento de las claves primarias compuestas
 */
@RunWith(AndroidJUnit4::class)
class CompositeKeyTest {

    private lateinit var database: PlayxyDatabase
    private lateinit var liveStreamDao: LiveStreamDao
    private lateinit var vodStreamDao: VodStreamDao
    private lateinit var seriesDao: SeriesDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            PlayxyDatabase::class.java
        ).allowMainThreadQueries().build()

        liveStreamDao = database.liveStreamDao()
        vodStreamDao = database.vodStreamDao()
        seriesDao = database.seriesDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun testSameStreamInMultipleCategories() = runBlocking {
        // Crear el mismo stream en 3 categorías diferentes
        val stream1 = createLiveStream("stream123", "Sports Channel", "cat_sports")
        val stream2 = createLiveStream("stream123", "Sports Channel", "cat_hd")
        val stream3 = createLiveStream("stream123", "Sports Channel", "cat_international")

        // Insertar las 3 instancias
        liveStreamDao.insertAll(listOf(
            EntityMapper.toEntity(stream1),
            EntityMapper.toEntity(stream2),
            EntityMapper.toEntity(stream3)
        ))

        // Verificar que se guardaron las 3 instancias
        val allStreams = liveStreamDao.getAllLiveStreams()
        assertEquals(3, allStreams.size)

        // Verificar que se pueden recuperar por streamId
        val streamsByStreamId = liveStreamDao.getLiveStreamsByStreamId("stream123")
        assertEquals(3, streamsByStreamId.size)

        // Verificar que se pueden recuperar por categoría
        val sportStreams = liveStreamDao.getLiveStreamsByCategory("cat_sports")
        assertEquals(1, sportStreams.size)
        assertEquals("stream123", sportStreams[0].streamId)

        val hdStreams = liveStreamDao.getLiveStreamsByCategory("cat_hd")
        assertEquals(1, hdStreams.size)
        assertEquals("stream123", hdStreams[0].streamId)
    }

    @Test
    fun testGetSpecificStreamInCategory() = runBlocking {
        // Crear streams
        val stream1 = createLiveStream("stream123", "Sports Channel", "cat_sports")
        val stream2 = createLiveStream("stream123", "Sports Channel", "cat_hd")
        val stream3 = createLiveStream("stream456", "News Channel", "cat_sports")

        liveStreamDao.insertAll(listOf(
            EntityMapper.toEntity(stream1),
            EntityMapper.toEntity(stream2),
            EntityMapper.toEntity(stream3)
        ))

        // Buscar stream específico en categoría específica
        val foundStream = liveStreamDao.getLiveStream("stream123", "cat_sports")
        assertNotNull(foundStream)
        assertEquals("stream123", foundStream?.streamId)
        assertEquals("cat_sports", foundStream?.categoryId)

        // Buscar stream que no existe en esa categoría
        val notFound = liveStreamDao.getLiveStream("stream456", "cat_hd")
        assertNull(notFound)
    }

    @Test
    fun testUpdateStreamInSpecificCategory() = runBlocking {
        // Crear el mismo stream en 2 categorías
        val stream1 = createLiveStream("stream123", "Original Name", "cat_sports")
        val stream2 = createLiveStream("stream123", "Original Name", "cat_hd")

        liveStreamDao.insertAll(listOf(
            EntityMapper.toEntity(stream1),
            EntityMapper.toEntity(stream2)
        ))

        // Actualizar solo en una categoría (usando REPLACE)
        val updatedStream = createLiveStream("stream123", "Updated Name", "cat_sports")
        liveStreamDao.insertAll(listOf(EntityMapper.toEntity(updatedStream)))

        // Verificar que solo se actualizó en cat_sports
        val sportsStream = liveStreamDao.getLiveStream("stream123", "cat_sports")
        assertEquals("Updated Name", sportsStream?.name)

        val hdStream = liveStreamDao.getLiveStream("stream123", "cat_hd")
        assertEquals("Original Name", hdStream?.name)
    }

    @Test
    fun testDeleteAllClearsAllInstances() = runBlocking {
        // Crear múltiples streams en múltiples categorías
        val streams = listOf(
            createLiveStream("stream1", "Channel 1", "cat1"),
            createLiveStream("stream1", "Channel 1", "cat2"),
            createLiveStream("stream2", "Channel 2", "cat1"),
            createLiveStream("stream3", "Channel 3", "cat3")
        )

        liveStreamDao.insertAll(streams.map { EntityMapper.toEntity(it) })

        // Verificar que se guardaron
        val allBefore = liveStreamDao.getAllLiveStreams()
        assertEquals(4, allBefore.size)

        // Borrar todo
        liveStreamDao.deleteAll()

        // Verificar que se borraron todas las instancias
        val allAfter = liveStreamDao.getAllLiveStreams()
        assertEquals(0, allAfter.size)
    }

    @Test
    fun testVodStreamsCompositeKey() = runBlocking {
        // Crear el mismo VOD en múltiples categorías
        val vod1 = createVodStream("vod123", "Movie Title", "cat_action")
        val vod2 = createVodStream("vod123", "Movie Title", "cat_hd")
        val vod3 = createVodStream("vod123", "Movie Title", "cat_new")

        vodStreamDao.insertAll(listOf(
            EntityMapper.toEntity(vod1),
            EntityMapper.toEntity(vod2),
            EntityMapper.toEntity(vod3)
        ))

        // Verificar que se guardaron las 3 instancias
        val allVods = vodStreamDao.getAllVodStreams()
        assertEquals(3, allVods.size)

        // Verificar consulta por categoría
        val actionVods = vodStreamDao.getVodStreamsByCategory("cat_action")
        assertEquals(1, actionVods.size)
    }

    @Test
    fun testSeriesCompositeKey() = runBlocking {
        // Crear la misma serie en múltiples categorías
        val series1 = createSeries("series123", "Series Title", "cat_drama")
        val series2 = createSeries("series123", "Series Title", "cat_popular")

        seriesDao.insertAll(listOf(
            EntityMapper.toEntity(series1),
            EntityMapper.toEntity(series2)
        ))

        // Verificar que se guardaron ambas instancias
        val allSeries = seriesDao.getAllSeries()
        assertEquals(2, allSeries.size)

        // Verificar consulta específica
        val dramaSeries = seriesDao.getSeriesByCategory("cat_drama")
        assertEquals(1, dramaSeries.size)
        assertEquals("series123", dramaSeries[0].seriesId)
    }

    @Test
    fun testDistinctStreamIds() = runBlocking {
        // Simular escenario real: múltiples streams, algunos repetidos en categorías
        val streams = listOf(
            createLiveStream("stream1", "Channel 1", "cat_sports"),
            createLiveStream("stream1", "Channel 1", "cat_hd"),
            createLiveStream("stream2", "Channel 2", "cat_news"),
            createLiveStream("stream3", "Channel 3", "cat_sports"),
            createLiveStream("stream3", "Channel 3", "cat_hd"),
            createLiveStream("stream3", "Channel 3", "cat_international")
        )

        liveStreamDao.insertAll(streams.map { EntityMapper.toEntity(it) })

        // Total de entradas
        val allStreams = liveStreamDao.getAllLiveStreams()
        assertEquals(6, allStreams.size)

        // Stream IDs únicos (para mostrar en UI sin duplicados)
        val uniqueStreamIds = allStreams.map { it.streamId }.distinct()
        assertEquals(3, uniqueStreamIds.size)
        assertTrue(uniqueStreamIds.containsAll(listOf("stream1", "stream2", "stream3")))
    }

    // Helper functions para crear objetos de prueba

    private fun createLiveStream(
        streamId: String,
        name: String,
        categoryId: String
    ) = LiveStream(
        streamId = streamId,
        name = name,
        streamIcon = null,
        isAdult = false,
        categoryId = categoryId,
        tvArchive = false,
        epgChannelId = null,
        added = null,
        customSid = null,
        directSource = null,
        tvArchiveDuration = 0
    )

    private fun createVodStream(
        streamId: String,
        name: String,
        categoryId: String
    ) = com.iptv.playxy.domain.VodStream(
        streamId = streamId,
        name = name,
        streamIcon = null,
        tmdbId = null,
        rating = 0.0f,
        rating5Based = 0.0f,
        containerExtension = "mp4",
        added = null,
        isAdult = false,
        categoryId = categoryId,
        customSid = null,
        directSource = null
    )

    private fun createSeries(
        seriesId: String,
        name: String,
        categoryId: String
    ) = com.iptv.playxy.domain.Series(
        seriesId = seriesId,
        name = name,
        cover = null,
        plot = null,
        cast = null,
        director = null,
        genre = null,
        releaseDate = null,
        rating = 0.0f,
        rating5Based = 0.0f,
        backdropPath = emptyList(),
        youtubeTrailer = null,
        episodeRunTime = null,
        categoryId = categoryId,
        tmdbId = null,
        lastModified = null
    )
}

