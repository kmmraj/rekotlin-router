package org.rekotlinrouter

import org.rekotlin.Action

class SetRouteAction(var route: Route,
                     var animated: Boolean = true,
                     action: StandardAction? = null): StandardActionConvertible {

    companion object {
        const val type = "RE_KOTLIN_ROUTER_SET_ROUTE"
    }

    init {
        // TODO: Convert the below to ArrayList
        if (action != null) {

            route = action.payload?.keys?.toTypedArray() as Route
            animated = action.payload["animated"] as Boolean
        }
    }

    override fun toStandardAction(): StandardAction {

        val payloadMap: HashMap<String,Any> = HashMap()
        payloadMap.put("route",this.route)
        payloadMap.put("animated",this.animated)
        return StandardAction(type = SetRouteAction.type,
                payload = payloadMap,
                isTypedAction = true) 
    }
}

class SetRouteSpecificData ( val route: Route, val data: Any): Action
