package com.example.polaroh1

import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.functions.BiFunction
import org.junit.Test

import org.junit.Assert.*
import java.util.concurrent.TimeUnit

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun concatMap() {

        Observable.just("A", "B", "C")
            .concatMap { i ->
                Observable.intervalRange(0, 3, 0, 1, TimeUnit.SECONDS)
                    .map { n -> "($n : $i)" }
            }.subscribe {
                println(it)
            }

        Thread.sleep(10000)
        //(0 : A)
        //(1 : A)
        //(2 : A)
        //(0 : B)
        //(1 : B)
        //(2 : B)
        //(0 : C)
        //(1 : C)
    }

    @Test
    fun flatMap() {
        Observable.just("A", "B", "C")
            .flatMap { i ->

                Observable.intervalRange(0, 3, 0, 1, TimeUnit.SECONDS)
                    .map { n -> "($n : $i)" }
            }.subscribe {

                println(it)


            }
        Thread.sleep(5000)

        //(0 : A)
        //(0 : C)
        //(0 : B)
        //(1 : A)
        //(1 : B)
        //(1 : C)
        //(2 : B)
        //(2 : A)
        //(2 : C)
    }

    @Test
    fun merge() {
        Observable.just(1, 2, 3, 4)
            /*.zipWith(Observable.just(Result("user1", 99), Result("user2", 77)), BiFunction { id, result ->
                UserData()
            })*/
           .zipWith(Observable.just("a","b","c","d","e"), BiFunction { id, result ->
                FinalData()
            })
            /*.map {

            }*/
            /*.mergeWith  (
//                Observable.just("a","b","c","d","e")
                Observable.just(1, 2, 3, 4)
            )*/
            //.buffer(1000,TimeUnit.MILLISECONDS)
            .subscribe {
                println("ericyu - ExampleUnitTest.zipWith, $it")
            }
    }

    data class Result(val id: String, val value: Int)
    data class UserData(val name: String="", val id: String="", val age: Int=0)
    data class FinalData(val code:Int=999)


}