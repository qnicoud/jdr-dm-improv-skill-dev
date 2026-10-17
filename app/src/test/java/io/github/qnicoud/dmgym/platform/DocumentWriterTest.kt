package io.github.qnicoud.dmgym.platform

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.FileNotFoundException

private const val AUTHORITY = "test.documents"

/** Fournisseur factice : l'ouverture échoue ou réussit, et les suppressions de document sont enregistrées. */
class FakeDocumentProvider : ContentProvider() {
    override fun onCreate() = true

    override fun openFile(
        uri: Uri,
        mode: String,
    ): ParcelFileDescriptor {
        if (failOpen) throw FileNotFoundException("boom")
        return ParcelFileDescriptor.open(File.createTempFile("doc", ".txt"), ParcelFileDescriptor.MODE_READ_WRITE)
    }

    override fun call(
        authority: String,
        method: String,
        arg: String?,
        extras: Bundle?,
    ): Bundle? {
        if (method == "android:deleteDocument") {
            @Suppress("DEPRECATION")
            deleted += extras?.getParcelable<Uri>("uri")
            check(!failDelete) { "cannot delete" }
        }
        return Bundle()
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(
        uri: Uri,
        values: ContentValues?,
    ): Uri? = null

    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<String>?,
    ) = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?,
    ) = 0

    companion object {
        var failOpen = false
        var failDelete = false
        val deleted = mutableListOf<Uri?>()
    }
}

@RunWith(RobolectricTestRunner::class)
class DocumentWriterTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val uri: Uri = DocumentsContract.buildDocumentUri(AUTHORITY, "doc1")

    @Before
    fun setUp() {
        FakeDocumentProvider.failOpen = false
        FakeDocumentProvider.failDelete = false
        FakeDocumentProvider.deleted.clear()
        Robolectric.setupContentProvider(FakeDocumentProvider::class.java, AUTHORITY)
    }

    @Test
    fun `successful write keeps the document`() =
        runTest {
            context.contentResolver.writeText(uri, "bonjour")

            assertEquals(emptyList<Uri?>(), FakeDocumentProvider.deleted)
        }

    @Test
    fun `failed write deletes the partial document and rethrows`() =
        runTest {
            FakeDocumentProvider.failOpen = true

            val failure = runCatching { context.contentResolver.writeText(uri, "x") }.exceptionOrNull()

            assertEquals(FileNotFoundException::class.java, failure?.javaClass)
            assertEquals(listOf<Uri?>(uri), FakeDocumentProvider.deleted)
        }

    @Test
    fun `failed cleanup does not mask the original failure`() =
        runTest {
            FakeDocumentProvider.failOpen = true
            FakeDocumentProvider.failDelete = true

            val failure = runCatching { context.contentResolver.writeText(uri, "x") }.exceptionOrNull()

            assertEquals(1, FakeDocumentProvider.deleted.size)
            assertEquals(FileNotFoundException::class.java, failure?.javaClass)
        }
}
