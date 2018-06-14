package org.rekotlinrouter


import org.assertj.core.api.Assertions.assertThat
import org.junit.Test


internal class ReKotlinRouterUnitTests {

    val mainActivityIdentifier = "MainActivity"
    val counterActivityIdentifier = "CounterActivity"
    val statsActivityIdentifier = "StatsActivity"
    val infoActivityIdentifier = "InfoActivity"


    @Test
    //@DisplayName("calculates transitions from an empty route to a multi segment route")
    fun test_transition_from_empty_to_multi_segment_route(){

        // Given
        val oldRoute: Route = arrayListOf()
        val newRoute = arrayListOf(mainActivityIdentifier, statsActivityIdentifier)

        // When
        val routingActions = Router.routingActionsForTransitionFrom(oldRoute, newRoute)

        // Then
        var action1Correct = false
        var action2Correct = false

        routingActions.forEach { routingAction ->
            when(routingAction) {
            is push -> {
                if(routingAction.responsibleRoutableIndex==0 && routingAction.segmentToBePushed == mainActivityIdentifier ){
                    action1Correct = true
                }
                if(routingAction.responsibleRoutableIndex==1 && routingAction.segmentToBePushed == statsActivityIdentifier ){
                    action2Correct = true
                }
            }
            }
        }
        assertThat(action1Correct).isTrue()
        assertThat(action2Correct).isTrue()
        assertThat(routingActions.count()).isEqualTo(2)
    }


    @Test
   // @DisplayName("generates a Change action on the last common subroute")
    fun test_change_action_on_last_common_subroute(){

        // Given
        val oldRoute = arrayListOf(mainActivityIdentifier,counterActivityIdentifier)
        val newRoute = arrayListOf(mainActivityIdentifier,statsActivityIdentifier)

        // When
        val routingActions = Router.routingActionsForTransitionFrom(oldRoute, newRoute)

        // Then
        var controllerIndex: Int = -1
        var toBeReplaced = ""
        var new = ""
        routingActions.forEach { routingAction ->

            when (routingAction) {
                is change -> {
                    controllerIndex = routingAction.responsibleRoutableIndex
                    toBeReplaced = routingAction.segmentToBeReplaced
                    new = routingAction.newSegment
                }
            }
        }
        assertThat(controllerIndex).isEqualTo(1)
        assertThat(toBeReplaced).isEqualTo(counterActivityIdentifier)
        assertThat(new).isEqualTo(statsActivityIdentifier)
    }

    @Test
   // @DisplayName("generates a Change action on the last common subroute, also for routes of different length")
    fun test_change_action_on_last_common_subroute_plus_routes_of_different_length(){

        // Given
        val oldRoute = arrayListOf(mainActivityIdentifier,counterActivityIdentifier)
        val newRoute = arrayListOf(mainActivityIdentifier,statsActivityIdentifier,infoActivityIdentifier)

        // When
        val routingActions = Router.routingActionsForTransitionFrom(oldRoute, newRoute)

        // Then
        var action1Correct = false
        var action2Correct = false

        routingActions.forEach { routingAction ->
            when(routingAction) {
                is change -> {
                    if(routingAction.responsibleRoutableIndex==1
                            && routingAction.segmentToBeReplaced == counterActivityIdentifier
                            && routingAction.newSegment == statsActivityIdentifier){
                        action1Correct = true
                    }
                }
                is push -> {
                    if(routingAction.responsibleRoutableIndex==2 && routingAction.segmentToBePushed == infoActivityIdentifier ){
                        action2Correct = true
                    }

                }

            }
        }

        assertThat(action1Correct).isTrue()
        assertThat(action2Correct).isTrue()
        assertThat(routingActions.count()).isEqualTo(2)
    }

    @Test
   // @DisplayName("generates a Change action on root when root element changes")
    fun test_change_action_on_root_when_root_element_changes(){

        // Given
        val oldRoute = arrayListOf(mainActivityIdentifier)
        val newRoute = arrayListOf(statsActivityIdentifier)

        // When
        val routingActions = Router.routingActionsForTransitionFrom(oldRoute, newRoute)

        // Then
        var controllerIndex: Int = -1
        var toBeReplaced = ""
        var new = ""
        routingActions.forEach { routingAction ->

            when (routingAction) {
                is change -> {
                    controllerIndex = routingAction.responsibleRoutableIndex
                    toBeReplaced = routingAction.segmentToBeReplaced
                    new = routingAction.newSegment
                }
            }
        }
        assertThat(controllerIndex).isEqualTo(0)
        assertThat(routingActions.count()).isEqualTo(1)
        assertThat(toBeReplaced).isEqualTo(mainActivityIdentifier)
        assertThat(new).isEqualTo(statsActivityIdentifier)
    }

    @Test
   // @DisplayName("generates a pop action followed by a change action on root when whole route changes")
    fun test_change_action_on_root_when_no_common_subroute() {
        // Given
        val oldRoute = arrayListOf(mainActivityIdentifier,counterActivityIdentifier)
        val newRoute = arrayListOf(statsActivityIdentifier)

        // When
        val routingActions = Router.routingActionsForTransitionFrom(oldRoute, newRoute)

        // Then
        var action1Correct = false
        var action2Correct = false

        routingActions.forEach { routingAction ->
            when(routingAction) {
                is pop -> {
                    if(routingAction.responsibleRoutableIndex == 1
                            && routingAction.segmentToBePopped == counterActivityIdentifier){
                        action1Correct = true
                    }
                }
                is change -> {
                    if(routingAction.responsibleRoutableIndex == 0
                            && routingAction.segmentToBeReplaced == mainActivityIdentifier
                            && routingAction.newSegment == statsActivityIdentifier){
                        action2Correct = true
                    }
                }
            }
        }

        assertThat(action1Correct).isTrue()
        assertThat(action2Correct).isTrue()
        assertThat(routingActions.count()).isEqualTo(2)
    }

    @Test
   // @DisplayName("calculates no actions for transition from empty route to empty route")
    fun test_no_action_when_transistion_from_empty_to_empty_route(){

        // Given
//        val oldRoute: Route = emptyArray()
//        val newRoute: Route = emptyArray()

        val oldRoute: Route = arrayListOf()
        val newRoute: Route = arrayListOf()
        // When
        val routingActions = Router.routingActionsForTransitionFrom(oldRoute, newRoute)

        // Then
        assertThat(routingActions.count()).isEqualTo(0)
    }

    @Test
   // @DisplayName("calculates no actions for transitions between identical, non-empty routes")
    fun test_no_action_when_transistion_from_identical_non_empty_routes(){

        // Given
        val oldRoute = arrayListOf(mainActivityIdentifier,counterActivityIdentifier)
        val newRoute = arrayListOf(mainActivityIdentifier,counterActivityIdentifier)

        // When
        val routingActions = Router.routingActionsForTransitionFrom(oldRoute, newRoute)

        // Then
        assertThat(routingActions.count()).isEqualTo(0)
    }

    @Test
    //@DisplayName("calculates transitions with multiple pops")
    fun test_transistion_with_multiple_pops(){

        // Given
        val oldRoute = arrayListOf(mainActivityIdentifier,statsActivityIdentifier,counterActivityIdentifier)
        val newRoute = arrayListOf(mainActivityIdentifier)

        // When
        val routingActions = Router.routingActionsForTransitionFrom(oldRoute, newRoute)

        // Then
        var action1Correct = false
        var action2Correct = false
        routingActions.forEach { routingAction ->
            when(routingAction) {
                is pop -> {
                    if(routingAction.responsibleRoutableIndex==2 && routingAction.segmentToBePopped == counterActivityIdentifier ){
                        action1Correct = true
                    }
                    if(routingAction.responsibleRoutableIndex==1 && routingAction.segmentToBePopped == statsActivityIdentifier ){
                        action2Correct = true
                    }
                }
            }
        }
        assertThat(action1Correct).isTrue()
        assertThat(action2Correct).isTrue()
        assertThat(routingActions.count()).isEqualTo(2)
    }

    @Test
   // @DisplayName("calculates transitions with multiple pushes")
    fun test_transistions_with_multiple_pushes(){

        // Given
        val oldRoute = arrayListOf(mainActivityIdentifier)
        val newRoute = arrayListOf(mainActivityIdentifier,statsActivityIdentifier,counterActivityIdentifier)

        // When
        val routingActions = Router.routingActionsForTransitionFrom(oldRoute, newRoute)

        // Then
        var action1Correct = false
        var action2Correct = false
        routingActions.forEach { routingAction ->
            when(routingAction) {
                is push -> {
                    if(routingAction.responsibleRoutableIndex==1 && routingAction.segmentToBePushed == statsActivityIdentifier ){
                        action1Correct = true
                    }
                    if(routingAction.responsibleRoutableIndex==2 && routingAction.segmentToBePushed == counterActivityIdentifier ){
                        action2Correct = true
                    }

                }
            }
        }
        assertThat(action1Correct).isTrue()
        assertThat(action2Correct).isTrue()
        assertThat(routingActions.count()).isEqualTo(2)
    }
}