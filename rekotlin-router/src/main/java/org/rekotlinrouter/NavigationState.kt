package org.rekotlinrouter

class FullRoute(route: Route) {
    val routeString: String

    init {
        this.routeString = route.joinToString(separator = "/")
    }
}

data class NavigationState(var route: Route = arrayListOf() ,
                       var routeSpecificState: HashMap<String,Any> = HashMap() ,
                       var changeRouteAnimated: Boolean = true) {

    fun <T> getRouteSpecificState(givenRoutes: Route): T? {
        val fullroute = FullRoute(givenRoutes)
        val routeString = fullroute.routeString

        return routeSpecificState[routeString] as? T
    }
}

interface HasNavigationState {
    var navigationState: NavigationState
}
