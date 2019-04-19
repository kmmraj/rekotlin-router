package org.rekotlinrouter

import org.rekotlin.Action

/**
This is ReKotlin's built in action type, it is the only built in type that conforms to the
`Action` protocol. `StandardAction` can be serialized and can therefore be used with developer
tools that restore state between app launches.


It is recommended that you define your own types that conform to `Action` - if you want to be able
to serialize your custom action types, you can implement `StandardActionConvertible` which will
make it possible to generate a `StandardAction` from your typed action - the best of both worlds!
 */
class StandardAction(val type: String,
                     val payload: Map<String, Any>? = null,
                     val isTypedAction: Boolean = false) : Action


/// Implement this protocol on your custom `Action` type if you want to make the action
/// serializable.
/// - Note: We are working on a tool to automatically generate the implementation of this protocol
///     for your custom action types.
interface StandardActionConvertible : Action {

    /**
    Within this initializer you need to use the payload from the `StandardAction` to configure the
    state of your custom action type.

    Example:

    ```
    init(_ standardAction: StandardAction) {
    this.twitterUser = standardAction.payload!["twitterUser"]!!
    }
    ```

    - Note: If you, as most developers, only use action serialization/deserialization during
    development, you can feel free to use the unsafe `!` operator.
     */


    //fun init( standardAction: StandardAction)

    /**
    Use the information from your custom action to generate a `StandardAction`. The `type` of the
    StandardAction should typically match the type name of your custom action type. You also need
    to set `isTypedAction` to `true`. Use the information from your action's properties to
    configure the payload of the `StandardAction`.

    Example:

    ```
    fun toStandardAction() -> StandardAction {
    val payload = ["twitterUser": this.twitterUser]

    return StandardAction(type = SearchTwitterScene.SetSelectedTwitterUser.type,
    payload = payload, isTypedAction = true)
    }
    ```

     */
    fun toStandardAction(): StandardAction
}
