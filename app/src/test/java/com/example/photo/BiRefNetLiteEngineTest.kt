package com.example.photo

import ai.onnxruntime.OnnxJavaType
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class BiRefNetLiteEngineTest {
    @Test
    fun testLiteModelLoadsAndInspectMetadata() {
        val modelFile = File("src/main/assets/birefnet_lite_fp16.onnx")
        val fileToLoad = if (modelFile.exists()) modelFile else File("app/src/main/assets/birefnet_lite_fp16.onnx")
        if (fileToLoad.exists() && fileToLoad.length() > 500) {
            val env = OrtEnvironment.getEnvironment()
            val session = env.createSession(fileToLoad.absolutePath, OrtSession.SessionOptions())
            println("Session created successfully!")
            println("Input Names: ${session.inputNames}")
            println("Output Names: ${session.outputNames}")
            for (name in session.inputNames) {
                println("Input [$name] info: ${session.inputInfo[name]}")
            }
            for (name in session.outputNames) {
                println("Output [$name] info: ${session.outputInfo[name]}")
            }
            session.close()
            env.close()
        }
    }

    @Test
    fun testLiteInferenceWithDummyInput() {
        val fileToLoad = File("src/main/assets/birefnet_lite_fp16.onnx")
        val target = if (fileToLoad.exists()) fileToLoad else File("app/src/main/assets/birefnet_lite_fp16.onnx")
        if (!target.exists() || target.length() <= 500) return

        val env = OrtEnvironment.getEnvironment()
        val session = env.createSession(target.absolutePath, OrtSession.SessionOptions())

        // Input shape: 1 x 3 x 512 x 512 (FLOAT32)
        val count = 1 * 3 * 512 * 512
        val byteBuffer = ByteBuffer.allocateDirect(count * 4)
            .order(ByteOrder.nativeOrder())
        val floatBuffer = byteBuffer.asFloatBuffer()

        for (i in 0 until count) {
            floatBuffer.put(0.5f)
        }
        byteBuffer.rewind()

        val shape = longArrayOf(1, 3, 512, 512)
        val tensor = OnnxTensor.createTensor(
            env,
            byteBuffer,
            shape,
            OnnxJavaType.FLOAT
        )

        println("Tensor created: type=${tensor.info.type}, shape=${tensor.info.shape.contentToString()}")

        val inputName = session.inputNames.iterator().next()
        val outputName = session.outputNames.iterator().next()
        val startTime = System.currentTimeMillis()
        try {
            val results = session.run(mapOf(inputName to tensor))
            val elapsed = System.currentTimeMillis() - startTime
            println("Inference completed in ${elapsed}ms!")

            val outputTensor = results.get(outputName).get() as OnnxTensor
            println("Output tensor: type=${outputTensor.info.type}, shape=${outputTensor.info.shape.contentToString()}")

            val outFloatBuffer = outputTensor.floatBuffer
            println("Output buffer capacity: ${outFloatBuffer.capacity()}")
            for (i in 0 until minOf(5, outFloatBuffer.capacity())) {
                val f = outFloatBuffer.get(i)
                println("Val $i: $f")
            }

            outputTensor.close()
            results.close()
        } catch (e: Exception) {
            println("Inference exception: ${e.message}")
            e.printStackTrace()
            throw e
        }

        tensor.close()
        session.close()
        env.close()
    }
}
