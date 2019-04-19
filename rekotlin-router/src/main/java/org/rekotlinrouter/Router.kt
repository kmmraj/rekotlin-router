package org.rekotlinrouter

import android.os.Handler
import android.os.Looper
import org.rekotlin.StateType
import org.rekotlin.Store
import org.rekotlin.StoreSubscriber
import org.rekotlin.Subscription


// TODO: Check is this need to be a singleton ?
sealed class RoutingAction
    data class push(val responsibleRoutableIndex: Int, val segmentToBePushed: RouteElementIdentifier): RoutingAction()
    data class pop(val responsibleRoutableIndex: Int, val segmentToBePopped: RouteElementIdentifier): RoutingAction()
    data class change(val responsibleRoutableIndex: Int, val segmentToBeReplaced: RouteElementIdentifier,
               val newSegment: RouteElementIdentifier): RoutingAction()


class Router<routerStateType: StateType> (var store: Store<routerStateType>,
                                          rootRoutable: Routable,
                                          stateTransform: (Subscription<routerStateType>) -> Subscription<NavigationState>): StoreSubscriber<NavigationState> {

    var lastNavigationState = NavigationState()
    // TODO: Collections.synchronizedList vs CopyOnWriteArrayList
    // var routables: List<Routable> = Collections.synchronizedList(arrayListOf<Routable>())
    var routables: ArrayList<Routable> = arrayListOf()

    init {
        this.routables.add(rootRoutable)
        this.store.subscribe(this,stateTransform)
    }

    private val mainThreadHandler = Handler(Looper.getMainLooper())

    override fun newState(state: NavigationState) {
        val routingActions = routingActionsForTransitionFrom(lastNavigationState.route,state.route)
        if (routingActions.size > 0) {
            routingActions.forEach { routingAction ->
                routingSerailActionHandler(routingAction, state)
            }
            lastNavigationState = state.copy()
        }
    }

    private fun routingSerailActionHandler(routingAction: RoutingAction, state: NavigationState) {

        synchronized(lock = routables){
                when(routingAction) {

                    is pop -> {
                        mainThreadHandler.post {
                            this.routables[routingAction.responsibleRoutableIndex]
                                    .popRouteSegment(routeElementIdentifier = routingAction.segmentToBePopped,
                                            animated = state.changeRouteAnimated) {}
                            this.routables.removeAt(routingAction.responsibleRoutableIndex+1)
                        }
                    }

                    is push -> {
                        mainThreadHandler.post {
                            val newRoutable = this.routables[routingAction.responsibleRoutableIndex].
                                    pushRouteSegment(routeElementIdentifier = routingAction.segmentToBePushed,
                                            animated = state.changeRouteAnimated){}
                            this.routables.add(newRoutable)
                        }

                    }

                    is change -> {
                        mainThreadHandler.post {
                            this.routables[routingAction.responsibleRoutableIndex + 1] =
                                    this.routables[routingAction.responsibleRoutableIndex].
                                            changeRouteSegment(from = routingAction.segmentToBeReplaced,
                                                    to = routingAction.newSegment,
                                                    animated = state.changeRouteAnimated){}
                        }
                    }
                }
        }
    }

    // Route Transformation Logic
    companion object {
    private fun largestCommonSubroute(oldRoute: Route, newRoute: Route): Int {
        var largestCommonSubroute = -1

        while(largestCommonSubroute + 1 < newRoute.count() &&
                largestCommonSubroute + 1 < oldRoute.count() &&
                newRoute[largestCommonSubroute + 1] == oldRoute[largestCommonSubroute + 1]){
                    largestCommonSubroute += 1
                }

        return largestCommonSubroute
    }


    // Maps Route index to Routable index. Routable index is offset by 1 because the root Routable
    // is not represented in the route, e.g.
    // route = ["tabBar"]
    // routables = [RootRoutable, TabBarRoutable]

    private fun routableIndexForRouteSegment(segment: Int): Int {
        return segment + 1
    }

    fun routingActionsForTransitionFrom(oldRoute: Route, newRoute: Route) : ArrayList<RoutingAction> {

        val routingActions = arrayListOf<RoutingAction>()

        // Find the last common subroute between two routes
        val commonSubroute = largestCommonSubroute(oldRoute, newRoute)

        if(commonSubroute == oldRoute.count() - 1 && commonSubroute == newRoute.count() - 1) {
            return arrayListOf()
        }
        // Keeps track which element of the routes we are working on
        // We start at the end of the old route
        var routeBuildingIndex = oldRoute.count() - 1

        // Pop all route segments of the old route that are no longer in the new route
        // Stop one element ahead of the commonSubroute. When we are one element ahead of the
        // commmon subroute we have three options:
        //
        // 1. The old route had an element after the commonSubroute and the new route does not
        //    we need to pop the route segment after the commonSubroute
        // 2. The old route had no element after the commonSubroute and the new route does, we
        //    we need to push the route segment(s) after the commonSubroute
        // 3. The new route has a different element after the commonSubroute, we need to replace
        //    the old route element with the new one
        while(routeBuildingIndex > commonSubroute + 1) {
            val routeSegmentToPop = oldRoute[routeBuildingIndex]

            val popAction = pop(routableIndexForRouteSegment(routeBuildingIndex - 1),
                    routeSegmentToPop)
           routingActions.add(popAction)
            routeBuildingIndex -= 1
        }

        // This is the 3. case:
        // "The new route has a different element after the commonSubroute, we need to replace
        //  the old route element with the new one"
        if((oldRoute.count() > (commonSubroute + 1))
                && (newRoute.count() > (commonSubroute + 1))) {
            val changeAction = change(routableIndexForRouteSegment(commonSubroute),
                    oldRoute[commonSubroute + 1],
                    newRoute[commonSubroute + 1])

            routingActions.add(changeAction)
        }
        // This is the 1. case:
        // "The old route had an element after the commonSubroute and the new route does not
        //  we need to pop the route segment after the commonSubroute"
        else if(oldRoute.count() > newRoute.count()) {
            val popAction = pop(routableIndexForRouteSegment(routeBuildingIndex - 1),
                    oldRoute[routeBuildingIndex])

            //routingActions = routingActions.plus(popAction)
            routingActions.add(popAction)
            routeBuildingIndex -= 1
        }

        // Push remainder of elements in new Route that weren't in old Route, this covers
        // the 2. case:
        // "The old route had no element after the commonSubroute and the new route does,
        //  we need to push the route segment(s) after the commonSubroute"
        val newRouteIndex = newRoute.count() - 1

        while(routeBuildingIndex < newRouteIndex) {
            val routeSegmentToPush = newRoute[routeBuildingIndex + 1]

            val pushAction = push(routableIndexForRouteSegment(routeBuildingIndex),
            routeSegmentToPush)

            routingActions.add(pushAction)
            routeBuildingIndex += 1
        }

        return routingActions
    }
    }
}




