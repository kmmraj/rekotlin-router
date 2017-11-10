package org.rekotlinrouter

import tw.geothings.rekotlin.Action



/**
The Navigation Reducer handles the state slice concerned with storing the current navigation
information. Note, that this reducer is **not** a *top-level* reducer, you need to use it within
another reducer and pass in the relevant state slice. Take a look at the testcases to see an
example set up.
 */
class NavigationReducer {

    companion object NavRed {

        fun handleAction(action: Action, state: NavigationState?): NavigationState {
          var navigationState = state ?: NavigationState()

          when(action)  {
            is SetRouteAction -> navigationState = setRoute(navigationState,action)
            is SetRouteSpecificData -> navigationState = setRouteSpecificData(navigationState, action.route,  action.data)
            else -> navigationState = NavigationState()
          }
          return navigationState
        }

        fun setRoute(state: NavigationState, setRouteAction: SetRouteAction): NavigationState {
            val navigationState = state

            navigationState.route = setRouteAction.route
            navigationState.changeRouteAnimated = setRouteAction.animated

            return navigationState
        }

        fun setRouteSpecificData(state: NavigationState, route: Route, data: Any): NavigationState {
            val routeString = FullRoute(route).routeString

            if (state.routeSpecificState.containsKey(routeString)) {
                state.routeSpecificState.replace(routeString, data)
            } else {
                state.routeSpecificState.put(routeString, data)
            }
            return state
        }

    }
}
