/**
 * Created by Mohanraj Karatadipalayam on 28/09/17.
 */

package org.rekotlinrouter

import android.os.Handler
import android.os.Looper
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.*
import org.mockito.stubbing.Answer
import org.powermock.api.mockito.PowerMockito
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Utility methods that unit tests can use to do common android library mocking that might be needed.
 */
object AndroidMockUtil {
    /**
     * Mocks main thread handler post() and postDelayed() for use in Android unit tests

     * To use this:
     *
     *  1. Call this method in an @Before method of your test.
     *  2. Place Looper.class in the @PrepareForTest annotation before your test class.
     *  3. any class under test that needs to call `new Handler(Looper.getMainLooper())` should be placed
     * in the @PrepareForTest annotation as well.
     *

     * @throws Exception
     */
    @Throws(Exception::class)
    fun mockMainThreadHandler() {

        // Mock the Looper
        PowerMockito.mockStatic(Looper::class.java)
        val mockMainThreadLooper = mock(Looper::class.java)
        `when`(Looper.getMainLooper()).thenReturn(mockMainThreadLooper)

        // Mock the Handler
        val mockMainThreadHandler = mock(Handler::class.java)

        val handlerPostAnswer = Answer { invocation ->
            val runnable = invocation.getArgument<Runnable>(0)
            var delay: Long? = 0L
            if (invocation.arguments.size > 1) {
                delay = invocation.getArgument<Long>(1)
            }
            if (runnable != null) {
                mainThread.schedule(runnable, delay!!, TimeUnit.MILLISECONDS)
            }
            true
        }

        doAnswer(handlerPostAnswer).`when`(mockMainThreadHandler).post(ArgumentMatchers.any(Runnable::class.java))
        doAnswer(handlerPostAnswer).`when`(mockMainThreadHandler).postDelayed(ArgumentMatchers.any(Runnable::class.java), ArgumentMatchers.anyLong())
        PowerMockito.whenNew(Handler::class.java).withArguments(mockMainThreadLooper).thenReturn(mockMainThreadHandler)
    }

    private val mainThread = Executors.newSingleThreadScheduledExecutor()

}

