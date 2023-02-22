package com.migueldev.wildrunning

import java.time.Duration

data class Runs(
    var date: String ?= null,
    var startTime: String ?= null,
    var user: String ?= null,
    var duration: String ?= null,

    var intervalMode: Boolean ?= null,
    var intervalDuration:  Int ?= null,
    var runingTime: String ?= null,
    var walkingTime: String ?= null,

    var ChallengeDuration: String ?= null,
    var ChallengeDistance: Double ?= null,

    var distance: Double ?= null,
    var maxSpeed: Double ?= null,
    var avgSpeed: Double ?= null,

    var centerLatitude: Double ?= null,
    var centerLongitude: Double ?= null,

    var sport: String ?= null,

    var activateGPS: Boolean ?= null

    )
