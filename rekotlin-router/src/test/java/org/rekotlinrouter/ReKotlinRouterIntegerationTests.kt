package org.rekotlinrouter

/**
 * Created by mkaratadipalayam on 28/09/17.
 */

import android.os.Handler
import android.os.Looper
import com.googlecode.junittoolbox.RunnableAssert
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import tw.geothings.rekotlin.Action
import tw.geothings.rekotlin.StateType
import tw.geothings.rekotlin.Store
import java.util.concurrent.TimeUnit


class FakeAppState: StateType {
    var navigationState = NavigationState()
}

fun appReducer(action: Action, state: FakeAppState?): FakeAppState {
    val fakeAppState =FakeAppState()
    fakeAppState.navigationState = NavigationReducer.handleAction(action, state?.navigationState)
    return fakeAppState
}

class MockRoutable: Routable {


    var callsToPushRouteSegment: Array<Pair< RouteElementIdentifier,Boolean>> = emptyArray()
    var callsToPopRouteSegment: Array<Pair< RouteElementIdentifier,Boolean>> = emptyArray()
    var callsToChangeRouteSegment: Array<Triple< RouteElementIdentifier,RouteElementIdentifier,Boolean>> = emptyArray()

    override fun pushRouteSegment(routeElementIdentifier: RouteElementIdentifier, animated: Boolean, completionHandler: RoutingCompletionHandler): Routable {
        callsToPushRouteSegment = callsToPushRouteSegment.plus(Pair(routeElementIdentifier,animated))
        completionHandler()
        return MockRoutable()
    }
    override fun popRouteSegment(routeElementIdentifier: RouteElementIdentifier, animated: Boolean, completionHandler: RoutingCompletionHandler) {
        callsToPopRouteSegment = callsToPopRouteSegment.plus(Pair(routeElementIdentifier,animated))
        completionHandler()
    }

    override fun changeRouteSegment(from: RouteElementIdentifier, to: RouteElementIdentifier, animated: Boolean, completionHandler: RoutingCompletionHandler): Routable {
        callsToChangeRouteSegment = callsToChangeRouteSegment.plus(Triple(from,to,animated))
        completionHandler()
        return MockRoutable()
    }


}



//@PrepareForTest(Looper::class)
//@RunWith(PowerMockRunner::class)
class ReKotlinRouterIntegerationTests {

    @PrepareForTest(Looper::class)
    @RunWith(PowerMockRunner::class)
    class RoutingCallTest{

    var store: Store<FakeAppState> = Store(reducer=::appReducer,state=FakeAppState())


    @Before
    @PrepareForTest(Looper::class,Handler::class,Router::class)
    fun initTest() {
        store = Store(reducer=::appReducer,state=FakeAppState())
        AndroidMockUtil.mockMainThreadHandler()
    }




    @Test
    // @DisplayName("does not request the main activity when no route is provided")
    fun test_no_request_main_activity_when_no_route_provided(){

         class FakeRootRoutable: Routable {
             var pushRouteIsCalled = false

             override fun pushRouteSegment(routeElementIdentifier: RouteElementIdentifier, animated: Boolean, completionHandler: RoutingCompletionHandler): Routable {
                 pushRouteIsCalled = true
                 return MockRoutable()
             }

             override fun popRouteSegment(routeElementIdentifier: RouteElementIdentifier, animated: Boolean, completionHandler: RoutingCompletionHandler) {
                 TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
             }

             override fun changeRouteSegment(from: RouteElementIdentifier, to: RouteElementIdentifier, animated: Boolean, completionHandler: RoutingCompletionHandler): Routable {
                 TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
             }
        }

        // Given
        val routable = FakeRootRoutable()
        // When
        Router(store = store, rootRoutable = routable) { subscription ->
            subscription.select { stateType -> stateType.navigationState }
        }
        // Then
        assertThat(routable.pushRouteIsCalled).isFalse()
    }

    //@DisplayName("requests the root with identifier when an initial route is provided")
   @Test
    fun test_requests_the_root_with_identifier_when_initial_route_provided(){

        // Given

        // The below syntax is not supported by powermock 😟
        // store.dispatch(SetRouteAction(arrayOf("MainActivity")))
        // https://github.com/powermock/powermock/issues/779
        // Until then
        val actionArray = arrayListOf("MainActivity")
        val action = SetRouteAction(actionArray)
        store.dispatch(action)

        class FakeRootRoutable(calledWithIdentifier: (RouteElementIdentifier?)-> Any) : Routable {
            var pushRouteIsCalled = false
            var calledWithIdentifier: ((RouteElementIdentifier?)-> Any)

            init {
                this.calledWithIdentifier = calledWithIdentifier
            }

            override fun pushRouteSegment(routeElementIdentifier: RouteElementIdentifier,
                                          animated: Boolean,
                                          completionHandler: RoutingCompletionHandler): Routable {
                calledWithIdentifier(routeElementIdentifier)
                completionHandler()
                pushRouteIsCalled = true
                return MockRoutable()
            }

            override fun popRouteSegment(routeElementIdentifier: RouteElementIdentifier, animated: Boolean, completionHandler: RoutingCompletionHandler) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun changeRouteSegment(from: RouteElementIdentifier, to: RouteElementIdentifier, animated: Boolean, completionHandler: RoutingCompletionHandler): Routable {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        }

        // Then

        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            object : RunnableAssert("Validate") {

                override fun run() {
                    var isRootElementCalled: Boolean = false
                    val rootRoutable = FakeRootRoutable { rootElementId: RouteElementIdentifier? ->
                        {
                            isRootElementCalled = rootElementId.equals("MainActivity")
                        }
                    }

                    Router(store = store, rootRoutable = rootRoutable) { subscription ->
                        subscription.select { stateType -> stateType.navigationState }
                    }
                    println("The value of isRootElementCalled is ${isRootElementCalled}")
                    assertThat(isRootElementCalled).isTrue()
                }
            }
        }

    }


    @Test
    //@Display("calls push on the root for a route with two elements")
    fun test_push_on_root_with_2_elements(){

        val actionArray = arrayListOf("MainActivity","SecondActivity")
        val action = SetRouteAction(actionArray)
        store.dispatch(action)

        class FakeChildRoutable(calledWithIdentifier: (RouteElementIdentifier?)-> Any) : Routable {
            var pushRouteIsCalled = false
            var calledWithIdentifier: ((RouteElementIdentifier?)-> Any)

            init {
                this.calledWithIdentifier = calledWithIdentifier
            }

            override fun pushRouteSegment(routeElementIdentifier: RouteElementIdentifier,
                                          animated: Boolean,
                                          completionHandler: RoutingCompletionHandler): Routable {
                calledWithIdentifier(routeElementIdentifier)
                completionHandler()
                pushRouteIsCalled = true
                return MockRoutable()
            }

            override fun popRouteSegment(routeElementIdentifier: RouteElementIdentifier, animated: Boolean, completionHandler: RoutingCompletionHandler) {
                TODO("Empty Implementation")
            }

            override fun changeRouteSegment(from: RouteElementIdentifier, to: RouteElementIdentifier, animated: Boolean, completionHandler: RoutingCompletionHandler): Routable {
                TODO("Empty Implementation")
            }
        }

        class FakeRootRoutable(var injectedRoutable: Routable) : Routable {
            var pushRouteIsCalled = false
            var rootRoutableIsCorrect = false
            var routeRootElementIdentifier: RouteElementIdentifier = ""

            override fun pushRouteSegment(routeElementIdentifier: RouteElementIdentifier,
                                          animated: Boolean,
                                          completionHandler: RoutingCompletionHandler): Routable {
                completionHandler()
                pushRouteIsCalled = true

                rootRoutableIsCorrect = routeElementIdentifier == "MainActivity"
                return injectedRoutable
            }

            override fun popRouteSegment(routeElementIdentifier: RouteElementIdentifier, animated: Boolean, completionHandler: RoutingCompletionHandler) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun changeRouteSegment(from: RouteElementIdentifier, to: RouteElementIdentifier, animated: Boolean, completionHandler: RoutingCompletionHandler): Routable {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        }



        // Then

        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            object : RunnableAssert("Validate") {


            override fun run() {
                var isChildIdentifierCorrect: Boolean = false
                val fakeChildRoutable = FakeChildRoutable { calledWithIdentifier: RouteElementIdentifier? -> {
                    isChildIdentifierCorrect = calledWithIdentifier.equals("SecondActivity")
                }
                }

                val fakeRootRoutable = FakeRootRoutable(injectedRoutable = fakeChildRoutable)

                Router(store = store, rootRoutable = fakeRootRoutable) { subscription ->
                    subscription.select { stateType -> stateType.navigationState }
                }
                println("The value of isIdentifierCorrect is ${isChildIdentifierCorrect}")
                println("The value of rootRouteElementIdentifier is ${fakeRootRoutable.routeRootElementIdentifier}")
                // Then
                // Assert
                assertThat(isChildIdentifierCorrect && fakeRootRoutable.rootRoutableIsCorrect).isTrue()
            }
        }
        }

    }
    }


    @PrepareForTest(Looper::class,Handler::class)
    @RunWith(PowerMockRunner::class)
    class RoutingSpecificDataTest{

        var store: Store<FakeAppState> = Store(reducer=::appReducer,state=null)

        @Before
        @PrepareForTest(Looper::class)
        fun initTest() {
            store = Store(reducer=::appReducer,state=null)
            AndroidMockUtil.mockMainThreadHandler()
        }

        //@DisplayName("allows accessing the data when providing the expected type")
        @Test
        fun test_allow_accessing_data_when_expected_type_provided(){

            //Given

            val actionArray = arrayListOf("MainActivity","SecondActivity")
            val actionData = SetRouteSpecificData(route = actionArray,data = "UserID_10")
            // When
            store.dispatch(actionData)

            // Then
            val data: String? = store.state.navigationState.getRouteSpecificState(actionArray)

            await().atMost(5, TimeUnit.SECONDS).untilAsserted { object : RunnableAssert("Validate") {
                override fun run() {
                    // Assert
                    assertThat(data).isEqualTo("UserID_10")
                }
            }
            }



        }


        @PrepareForTest(Looper::class,Handler::class,Router::class)
        @RunWith(PowerMockRunner::class)
        class RoutingAnimationTest{

            var store: Store<FakeAppState> = Store(reducer=::appReducer,state=null)
            var mockRoutable: MockRoutable = MockRoutable()
            var router: Router<FakeAppState>? = null

            @Before
            @PrepareForTest(Looper::class,Handler::class,Router::class)
            fun initTest() {
                AndroidMockUtil.mockMainThreadHandler()
                store = Store(reducer = ::appReducer, state = null)
                mockRoutable = MockRoutable()

                router = Router(store = store,
                        rootRoutable = mockRoutable) { subscription ->
                    subscription.select { stateType ->
                        stateType.navigationState
                    }
                }
            }

            @Test
           // @DisplayName("when dispatching an animated route change")
            fun test_dipatching_animated_route_change_with_animate_as_true(){
                //Given

                val actionArray = arrayListOf("MainActivity","SecondActivity")
                val action = SetRouteAction(actionArray,animated = true)
                // When
                store.dispatch(action)

                // Then

                await().atMost(5, TimeUnit.SECONDS).untilAsserted { object : RunnableAssert("Validate") {
                    @PrepareForTest(Looper::class,Handler::class)
                    override fun run() {
                        // Assert
                        assertThat(mockRoutable.callsToPushRouteSegment.last().second).isTrue()
                    }
                }}

            }


            @Test // @DisplayName("when dispatching an unanimated route change")
            fun test_dipatching_animated_route_change_with_animate_as_false(){
                //Given

                val actionArray = arrayListOf("MainActivity","SecondActivity")
                val action = SetRouteAction(actionArray,animated = false)
                // When
                store.dispatch(action)

                // Then

                await().atMost(5, TimeUnit.SECONDS).untilAsserted { object : RunnableAssert("Validate") {
                    override fun run() {
                        // Assert
                        assertThat(mockRoutable.callsToPushRouteSegment.last().second).isFalse()
                    }
                }}

            }

            @Test // @DisplayName("when dispatching an default route change")
            fun test_dipatching_animated_route_change_with_animate_with_default(){
                //Given

                val actionArray = arrayListOf("MainActivity","SecondActivity")
                val action = SetRouteAction(actionArray)
                // When
                store.dispatch(action)

                // Then

                await().atMost(5, TimeUnit.SECONDS).untilAsserted { object : RunnableAssert("Validate") {
                    override fun run() {
                        // Assert
                        assertThat(mockRoutable.callsToPushRouteSegment.last().second).isTrue()
                    }
                }}

            }

        }

    }



}


