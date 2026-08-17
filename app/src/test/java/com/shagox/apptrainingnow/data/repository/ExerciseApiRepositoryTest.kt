package com.shagox.apptrainingnow.data.repository

import com.shagox.apptrainingnow.data.remote.ExerciseApi
import com.shagox.apptrainingnow.data.remote.dto.CategoryDto
import com.shagox.apptrainingnow.data.remote.dto.ExerciseDto
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests unitarios para ExerciseApiRepository.
 * Verifica que los datos de la API se mapean correctamente a entidades.
 */
class ExerciseApiRepositoryTest {

    private val mockApi = mockk<ExerciseApi>()

    @Test
    fun getCategoryStats_returnsCategoriesFromApi() {
        runBlocking {
            // getCategoryStats() usa /api/categories (entidad propia), no agrupa /api/exercises.
            val apiCategories = listOf(
                CategoryDto(id = 1, name = "Fuerza", exerciseCount = 2),
                CategoryDto(id = 2, name = "Piernas", exerciseCount = 1)
            )
            coEvery { mockApi.getCategories() } returns apiCategories

            val repo = ExerciseApiRepository(api = mockApi)
            val stats = repo.getCategoryStats().first()

            assertEquals(2, stats.size) // Fuerza (2), Piernas (1)
            val fuerza = stats.find { it.category == "Fuerza" }!!
            assertEquals(2, fuerza.count)
            val piernas = stats.find { it.category == "Piernas" }!!
            assertEquals(1, piernas.count)
        }
    }

    @Test
    fun getExercisesByCategory_returnsMappedEntities() {
        runBlocking {
            val apiExercises = listOf(
                ExerciseDto(1, "Press Banca", "Fuerza", "Desc", null, "http://img.com/1.jpg")
            )
            coEvery { mockApi.getExercisesByCategory("Fuerza") } returns apiExercises

            val repo = ExerciseApiRepository(api = mockApi)
            val exercises = repo.getExercisesByCategory("Fuerza").first()

            assertEquals(1, exercises.size)
            assertEquals(1, exercises[0].id)
            assertEquals("Press Banca", exercises[0].name)
            assertEquals("Fuerza", exercises[0].category)
        }
    }

    @Test
    fun observeExercise_returnsEntityWhenFound() {
        runBlocking {
            val dto = ExerciseDto(1, "Press Banca", "Fuerza", "Desc", "http://video.com", "http://img.com/1.jpg")
            coEvery { mockApi.getExerciseById(1) } returns dto

            val repo = ExerciseApiRepository(api = mockApi)
            val exercise = repo.observeExercise(1).first()

            assertEquals(1, exercise!!.id)
            assertEquals("Press Banca", exercise.name)
            assertEquals("http://video.com", exercise.videoUrl)
        }
    }
}
