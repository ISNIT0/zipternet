//https://github.com/commonsguy/cw-omnibus/blob/master/ContentProvider/Pipe/app/src/main/java/com/commonsware/android/cp/pipe/PipeProvider.java

package app.zipternet.custom

/***
 * Copyright (c) 2012 CommonsWare, LLC
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0. Unless required
 * by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * Covered in detail in the book _The Busy Coder's Guide to Android Development_
 * https://commonsware.com/Android
 */


import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.*
import java.net.URLDecoder
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class ZipProvider : AbstractFileProvider() {
    public var CONTENT_URI:String = ""
    companion object {
        public var zipFile: ZipFile? = null
    }

    override fun onCreate(): Boolean {
        CONTENT_URI = "content://${BuildConfig.APPLICATION_ID}/"
        return true
    }

    fun setZipFile(file: File) {
        zipFile = ZipFile(file)
    }

    fun getEntry(name:String): ZipEntry? {
        return zipFile!!.getEntry(name)
    }

    fun getInputStream(entry:ZipEntry):InputStream? {
        return zipFile!!.getInputStream(entry)
    }

    fun getTextContent(name:String): String? {
        val entry = zipFile!!.getEntry(name)
        if(entry === null) return null
        val inpStream = zipFile!!.getInputStream(entry)
        val isReader = InputStreamReader(inpStream)
        val br = BufferedReader(isReader)
        return br.use { it.readText() }
    }

    override fun openFile(uri: Uri, mode: String?): ParcelFileDescriptor? {
        val pipe: Array<ParcelFileDescriptor> = ParcelFileDescriptor.createPipe()

        val path = URLDecoder.decode(uri.path).substring(1)

        val entry = zipFile!!.getEntry(path)
        val dataStream = zipFile!!.getInputStream(entry)

        TransferThread(
            dataStream,
            ParcelFileDescriptor.AutoCloseOutputStream(pipe[1])
        ).start()

        return pipe[0]
    }

    internal class TransferThread(var inputStream: InputStream, var outputStream: OutputStream) : Thread() {

        override fun run() {

            inputStream.use { inStream ->
                outputStream.use { outStream ->
                    inStream.copyTo(outStream)
                }
            }

            outputStream.flush()

        }
    }
}