
# ReKotlin-Router [![Build Status](https://travis-ci.org/ReKotlin/rekotlin-router.svg?branch=master)](https://travis-ci.org/ReKotlin/rekotlin-router) [![License MIT](https://img.shields.io/badge/license-MIT-blue.svg?style=flat-square)](https://github.com/ReKotlin/rekotlin-router/blob/master/LICENSE.md) [ ![Download](https://api.bintray.com/packages/kmmraj/rekotlin-router/rekotlin-router/images/download.svg?version=0.1.8) ](https://bintray.com/kmmraj/rekotlin-router/rekotlin-router/0.1.8/link)

A declarative router for [ReKotlin](https://github.com/GeoThings/ReKotlin). Allows developers to declare routes in a similar manner as URLs are used on the web.

Using ReKotlinRouter you can navigate your app by defining the target location in the form of a URL-like sequence of identifiers:

```Kotlin
val routes = arrayListOf(loginRoute, repoListRoute, repoDetailRoute)
val actionData =  SetRouteSpecificData(route = routes, data = "somedata")
val action = SetRouteAction(route = routes)
mainStore.dispatch(actionData)
mainStore.dispatch(action)
```

# About ReKotlinRouter


When building apps with ReKotlin you should aim to cause **all** state changes through actions - this includes changes to the navigation state.

This requires to store the current navigation state within the app state and to use actions to trigger changes to that state - both is provided ReKotlinRouter.

# Installation
Add the following line along with ReKotlin dependencies in gradle file

```Groovy
implementation 'org.rekotlinrouter:rekotlin-router:0.1.0'
```

# Configuration

Extend your app state to include the navigation state:

```Kotlin
import org.rekotlinrouter.HasNavigationState
import org.rekotlinrouter.NavigationState
import tw.geothings.rekotlin.StateType

data class AppState(override var navigationState: NavigationState,
                         // other application states such as....
                          var authenticationState: AuthenticationState,
                          var repoListState: RepoListState): StateType, HasNavigationState

```

After you've initialized your store, create an instance of `Router`, passing in a reference to the store and to the root `Routable`. Additionally you will need to provide a closure that describes how to access the `navigationState` of your application state:

```Kotlin
 router = Router(store = mainStore,
                rootRoutable = RootRoutable(context = applicationContext),
                stateTransform = { subscription ->
                    subscription.select { stateType ->
                        stateType.navigationState
                    }
                })
```

We'll discuss `Routable` in the next main section.

## Calling the Navigation Reducer

The `NavigationReducer` is provided as part of `ReKotlinRouter`. You need to call it from within your top-level reducer. Here's a simple example from the specs:

```Kotlin

fun appReducer(action: Action, oldState: GitHubAppState?) : GitHubAppState {

    // if no state has been provided, create the default state
    val state = oldState ?: GitHubAppState(
            navigationState = NavigationReducer.handleAction(action = action, state = oldState?.navigationState),
             // other application state reducers such as....
            authenticationState = AuthenticationState(loggedInState = LoggedInState.loggedIn,
                    userName = ""),
            repoListState = RepoListState())

    return state.copy(
            navigationState = (::navigationReducer)(action, state.navigationState),
             // other application state reducers such as....
            authenticationState = (::authenticationReducer)(action, state.authenticationState),
            repoListState = (::repoListReducer)(action, state.repoListState))
}

fun navigationReducer(action: Action, oldState: NavigationState?): NavigationState {
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

```
This will make reducer handle all routing relevant actions.

# Implementing `Routable`

ReKotlinRouter works with routes that are defined, similar to URLs, as a sequence of identifiers e.g. `["Home", "User", "UserDetail"]`. It uses `Routable`s to implement that interaction.

Each route segment is mapped to one responsible `Routable`. The `Routable` needs to be able to present a child, hide a child or replace a child with another child.

Here is the `Routable` protocol with the methods you should implement:

```Kotlin
interface Routable {

    fun pushRouteSegment(routeElementIdentifier: RouteElementIdentifier,
                         animated: Boolean,
                         completionHandler: RoutingCompletionHandler): Routable

    fun popRouteSegment(routeElementIdentifier: RouteElementIdentifier,
                        animated: Boolean,
                        completionHandler: RoutingCompletionHandler)

    fun changeRouteSegment(from: RouteElementIdentifier,
                           to: RouteElementIdentifier,
                           animated: Boolean,
                           completionHandler: RoutingCompletionHandler): Routable
}
```

As part of initializing `Router` you need to pass the first `Routable` as an argument. That root `Routable` will be responsible for the first route segment.

If e.g. you set the route of your application to `["Home"]`, your root `Routable` will be asked to present the view that corresponds to the identifier `"Home"`.


Whenever a `Routable` presents a new route segment, it needs to return a new `Routable` that will be responsible for managing the presented segment. If you want to navigate from `["Home"]` to `["Home", "Users"]` the `Routable` responsible for the `"Home"` segment will be asked to present the `"User"` segment.

If your navigation stack uses a modal presentation for this transition, the implementation of `Routable` for the `"Root"` segment might look like this:

```Kotlin

class RootRoutable(val context: Context): Routable {
    override fun popRouteSegment(routeElementIdentifier: RouteElementIdentifier,
                                 animated: Boolean,
                                 completionHandler: RoutingCompletionHandler) {
    }

    override fun pushRouteSegment(routeElementIdentifier: RouteElementIdentifier,
                                  animated: Boolean,
                                  completionHandler: RoutingCompletionHandler): Routable {
        if(routeElementIdentifier == loginRoute) {
            return LoginRoutable(context)
        } else if (routeElementIdentifier == welcomeRoute) {
            return RoutableHelper.createWelcomeRoutable(context)
        }

        return LoginRoutable(context)
    }

    override fun changeRouteSegment(from: RouteElementIdentifier,
                                    to: RouteElementIdentifier,
                                    animated: Boolean,
                                    completionHandler: RoutingCompletionHandler): Routable {
       TODO("not implemented")
    }

}

object RoutableHelper {

     fun createWelcomeRoutable(context: Context): WelcomeRoutable {
        val welcomeIntent = Intent(context, WelcomeActivity::class.java)
        welcomeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(welcomeIntent)
        return WelcomeRoutable(context)
    }
}

```

## Calling the Completion Handler within Routables

ReKotlinRouter needs to throttle the navigation actions, since many UI frameworks don't allow to perform multiple navigation steps in parallel. Therefor every method of `Routable` receives a `completionHandler`. The router will not perform any further navigation actions until the completion handler is called.

# Changing the Current Route

Currently the only way to change the current application route is by using the `SetRouteAction` and providing an absolute route. Here's a brief example:

```Kotlin
 mEmailSignInButton.setOnClickListener {
            mainStore.dispatch(LoginAction(userName = mETEmail.text.toString(),
                    password = mETPassword.text.toString()))
        }
```
As development continues, support for changing individual route segments will be added.

## Bugs and Feedback

For bugs, feature requests, and discussion please use [GitHub Issues][issues].
For general usage questions please use the [mailing list][list] or [StackOverflow][so].

# Contributing

There's still a lot of work to do here! We would love to see you involved!

### Submitting patches

The best way to submit a patch is to [fork the project on github](https://help.github.com/articles/fork-a-repo/) then send us a
[pull request](https://help.github.com/articles/creating-a-pull-request/) via [github](https://github.com).

If you create your own fork, it might help to enable rebase by default
when you pull by executing
``` bash
git config --global pull.rebase true
```
This will avoid your local repo having too many merge commits
which will help keep your pull request simple and easy to apply.

Before submitting the pull request, make sure all existing tests are passing, and add the new test if it is required.

### New functionality
If you want to add new functionality, please file a new proposal issue first to make sure that it is not in progress already. If you have any questions, feel free to create a question issue.

You can find all the details on how to get started in the [Contributing Guide](/CONTRIBUTING.md).

## Compiling & Running tests

 To build or test any of the targets, run `gradle assemble`.

# Example Projects

- [GitHubExample](https://github.com/kmmraj/rekotlin-router-github-example): A real world example, involving authentication, network requests and navigation. Still WIP but should be the best example when you starting to adapt `ReKotlin` and `Rekotlin-Router` in your own app.
- [Router working with Fragments](https://github.com/ReKotlin/reKotlinFragmentExample): An example, that explains how to use the router along with fragments.

## Credits

- Many thanks to [Benjamin Encz](https://github.com/Ben-G) and other ReSwift contributors for buidling original [ReSwift](https://github.com/ReSwift/ReSwift) that we really enjoyed working with.
- Also huge thanks to [Dan Abramov](https://github.com/gaearon) for building [Redux](https://github.com/reactjs/redux) - all ideas in here and many implementation details were provided by his library.