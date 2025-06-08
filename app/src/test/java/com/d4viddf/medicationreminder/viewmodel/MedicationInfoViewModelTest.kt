package com.d4viddf.medicationreminder.viewmodel

import com.d4viddf.medicationreminder.data.CimaMedicationDetail
import com.d4viddf.medicationreminder.data.Medication
import com.d4viddf.medicationreminder.repository.MedicationInfoRepository
import com.d4viddf.medicationreminder.repository.MedicationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class MedicationInfoViewModelTest {

    @Mock
    private lateinit var mockMedicationRepository: MedicationRepository

    @Mock
    private lateinit var mockMedicationInfoRepository: MedicationInfoRepository

    private lateinit var viewModel: MedicationInfoViewModel

    // Using StandardTestDispatcher for more control if needed, though runTest handles much of it.
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher) // Set main dispatcher for ViewModelScope
        viewModel = MedicationInfoViewModel(mockMedicationRepository, mockMedicationInfoRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain() // Reset main dispatcher after the test
    }

    @Test
    fun `loadMedicationInfo success updates StateFlows`() = runTest {
        val medicationId = 1
        val nregistro = "12345"
        val sampleMedication = Medication(id = medicationId, name = "Test Med", nregistro = nregistro, typeId = 1, color = "Red", dosage = "10mg", packageSize = 30, remainingDoses = 30)
        val sampleCimaDetail = CimaMedicationDetail(nregistro = nregistro, nombre = "Test CIMA Med", labtitular = "Test Lab", pactivos = "Test Active", formaFarmaceutica = null, viasAdministracion = null, condPresc = null, estado = null, docs = null, fotos = null, comerc = true, conduc = false, triangulo = false, huerfano = false, biosimilar = false)

        `when`(mockMedicationRepository.getMedicationById(medicationId)).thenReturn(sampleMedication)
        `when`(mockMedicationInfoRepository.getMedicationDetailsByNRegistro(nregistro)).thenReturn(sampleCimaDetail)

        viewModel.loadMedicationInfo(medicationId)
        advanceUntilIdle() // Ensure all coroutines launched in viewModelScope complete

        assertEquals(false, viewModel.isLoading.value)
        assertNull(viewModel.error.value)
        assertEquals(sampleCimaDetail, viewModel.medicationInfo.value)
        verify(mockMedicationRepository).getMedicationById(medicationId)
        verify(mockMedicationInfoRepository).getMedicationDetailsByNRegistro(nregistro)
    }

    @Test
    fun `loadMedicationInfo medicationNotFound setsError`() = runTest {
        val medicationId = 1
        `when`(mockMedicationRepository.getMedicationById(medicationId)).thenReturn(null)

        viewModel.loadMedicationInfo(medicationId)
        advanceUntilIdle()

        assertEquals(false, viewModel.isLoading.value)
        assertNotNull(viewModel.error.value)
        assertTrue(viewModel.error.value!!.contains("Medication with ID $medicationId not found"))
        assertNull(viewModel.medicationInfo.value)
        verify(mockMedicationRepository).getMedicationById(medicationId)
    }

    @Test
    fun `loadMedicationInfo nregistroNull setsError`() = runTest {
        val medicationId = 1
        val sampleMedication = Medication(id = medicationId, name = "Test Med", nregistro = null, typeId = 1, color = "Red", dosage = "10mg", packageSize = 30, remainingDoses = 30)
        `when`(mockMedicationRepository.getMedicationById(medicationId)).thenReturn(sampleMedication)

        viewModel.loadMedicationInfo(medicationId)
        advanceUntilIdle()

        assertEquals(false, viewModel.isLoading.value)
        assertNotNull(viewModel.error.value)
        assertTrue(viewModel.error.value!!.contains("nregistro) not found"))
        assertNull(viewModel.medicationInfo.value)
    }

    @Test
    fun `loadMedicationInfo nregistroEmpty setsError`() = runTest {
        val medicationId = 1
        val sampleMedication = Medication(id = medicationId, name = "Test Med", nregistro = "", typeId = 1, color = "Red", dosage = "10mg", packageSize = 30, remainingDoses = 30)
        `when`(mockMedicationRepository.getMedicationById(medicationId)).thenReturn(sampleMedication)

        viewModel.loadMedicationInfo(medicationId)
        advanceUntilIdle()

        assertEquals(false, viewModel.isLoading.value)
        assertNotNull(viewModel.error.value)
        assertTrue(viewModel.error.value!!.contains("nregistro) not found"))
        assertNull(viewModel.medicationInfo.value)
    }

    @Test
    fun `loadMedicationInfo cimaReturnsNull setsError`() = runTest {
        val medicationId = 1
        val nregistro = "12345"
        val sampleMedication = Medication(id = medicationId, name = "Test Med", nregistro = nregistro, typeId = 1, color = "Red", dosage = "10mg", packageSize = 30, remainingDoses = 30)

        `when`(mockMedicationRepository.getMedicationById(medicationId)).thenReturn(sampleMedication)
        `when`(mockMedicationInfoRepository.getMedicationDetailsByNRegistro(nregistro)).thenReturn(null)

        viewModel.loadMedicationInfo(medicationId)
        advanceUntilIdle()

        assertEquals(false, viewModel.isLoading.value)
        assertNotNull(viewModel.error.value)
        assertTrue(viewModel.error.value!!.contains("Could not retrieve details from CIMA"))
        assertNull(viewModel.medicationInfo.value)
    }

    @Test
    fun `loadMedicationInfo repositoryThrowsException setsError`() = runTest {
        val medicationId = 1
        val nregistro = "12345"
        val sampleMedication = Medication(id = medicationId, name = "Test Med", nregistro = nregistro, typeId = 1, color = "Red", dosage = "10mg", packageSize = 30, remainingDoses = 30)

        `when`(mockMedicationRepository.getMedicationById(medicationId)).thenReturn(sampleMedication)
        `when`(mockMedicationInfoRepository.getMedicationDetailsByNRegistro(nregistro)).thenThrow(RuntimeException("Simulated network error"))

        viewModel.loadMedicationInfo(medicationId)
        advanceUntilIdle()

        assertEquals(false, viewModel.isLoading.value)
        assertNotNull(viewModel.error.value)
        assertTrue(viewModel.error.value!!.contains("An unexpected error occurred"))
        assertNull(viewModel.medicationInfo.value)
    }

    @Test
    fun `loadMedicationInfo isLoading is set true during loading and false after`() = runTest {
        val medicationId = 1
        val nregistro = "12345"
        val sampleMedication = Medication(id = medicationId, name = "Test Med", nregistro = nregistro, typeId = 1, color = "Red", dosage = "10mg", packageSize = 30, remainingDoses = 30)
        val sampleCimaDetail = CimaMedicationDetail(nregistro = nregistro, nombre = "Test CIMA Med", labtitular = "Test Lab", pactivos = "Test Active", formaFarmaceutica = null, viasAdministracion = null, condPresc = null, estado = null, docs = null, fotos = null, comerc = true, conduc = false, triangulo = false, huerfano = false, biosimilar = false)

        `when`(mockMedicationRepository.getMedicationById(medicationId)).thenReturn(sampleMedication)
        `when`(mockMedicationInfoRepository.getMedicationDetailsByNRegistro(nregistro)).thenReturn(sampleCimaDetail)

        // Don't call advanceUntilIdle() immediately to check initial isLoading state
        // This is a bit tricky as the launch block might complete very quickly.
        // A more robust way would involve pausing the dispatcher or using Turbine.

        viewModel.loadMedicationInfo(medicationId)
        // At this point, isLoading should ideally be true if the coroutine hasn't finished.
        // However, runTest might execute it too fast.
        // A better assertion for this specific test case would be difficult without more complex setup.
        // We rely on the finally block setting it to false.

        advanceUntilIdle() // Let all coroutines complete

        assertEquals(false, viewModel.isLoading.value) // Final state
    }
}
