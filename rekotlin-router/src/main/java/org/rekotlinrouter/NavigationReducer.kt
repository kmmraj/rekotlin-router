package org.rekotlinrouter

import org.rekotlin.Action

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
            is SetRouteAction -> {
                navigationState = setRoute(navigationState,action)
            }
            is SetRouteSpecificData -> {
                navigationState = setRouteSpecificData(navigationState, action.route,  action.data)
            }
            else -> {
                navigationState = NavigationState()
            }
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

            state.routeSpecificState[routeString] = data

            return state
        }

        fun reduce(action: Action, oldState: NavigationState?): NavigationState {
            val state =  oldState ?: NavigationReducer.handleAction(action = action, state = oldState)
            when (action) {
                is SetRouteAction -> {
                    return NavigationReducer.handleAction(action = action, state = state)
                }

                is SetRouteSpecificData -> {
                    return NavigationReducer.handleAction(action = action, state = state)
                }
            }
            return state
        }

    }
}
