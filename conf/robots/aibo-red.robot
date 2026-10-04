{
  "name": "AIBO ERS-7",
  "icon": [
    {
      "xi": -0.1,
      "yi": 0.105,
      "xf": 0.03934620572061251,
      "yf": 0.11972996565876079
    },
    {
      "xi": 0.17,
      "yi": 0.1,
      "xf": 0.03934620572061251,
      "yf": 0.11972996565876079
    },
    {
      "xi": -0.1,
      "yi": 0.105,
      "xf": -0.125,
      "yf": -0.0
    },
    {
      "xi": -0.125,
      "yi": -0.0,
      "xf": -0.09608605051634642,
      "yf": -0.10599046140283741
    },
    {
      "xi": -0.09608605051634642,
      "yi": -0.10599046140283741,
      "xf": 0.05,
      "yf": -0.12
    },
    {
      "xi": 0.17,
      "yi": 0.08,
      "xf": 0.17,
      "yf": 0.1
    },
    {
      "xi": 0.17,
      "yi": 0.08,
      "xf": 0.12,
      "yf": 0.06
    },
    {
      "xi": 0.05,
      "yi": -0.12,
      "xf": 0.17,
      "yf": -0.1
    },
    {
      "xi": 0.17,
      "yi": -0.08,
      "xf": 0.17,
      "yf": -0.1
    },
    {
      "xi": 0.12,
      "yi": -0.06,
      "xf": 0.12,
      "yf": 0.06
    },
    {
      "xi": 0.17,
      "yi": -0.08,
      "xf": 0.12,
      "yf": -0.06
    }
  ],
  "boundingBox": {
    "xmin": -0.125,
    "ymin": -0.12,
    "xmax": 0.2,
    "ymax": 0.11972996565876079
  },
  "image": "./conf/2dmodels/aibo-red.png",
  "shapeParts": "./conf/3dmodels/aibo",
  "team": "RED",
  "kinematics": {
    "drive": "tc.vrobot.models.ArticulatedDrive",
    "walking": "tcrob.umu.soccer.walking.AiboWalking",
    "model": "./conf/robots/aibo.kine",
    "vmax": 0.451,
    "umax": 0.344,
    "rmax": 200.0,
    "lamax": 0.0,
    "ldmax": 0.0,
    "rwheel": 0.0,
    "skid": 1.0,
    "gear": 0.0,
    "pulses": 0.0,
    "odomET": 0.0,
    "odomER": 0.0,
    "odomBias": 0.0
  },
  "sensors": {
    "son": {
      "simerror": 0.05,
      "sensors": []
    },
    "ir": {
      "simerror": 0.05,
      "sensors": []
    },
    "lrf": {
      "simerror": 0.05,
      "sensors": []
    },
    "lsb": {
      "simerror": 0.05,
      "sensors": []
    },
    "trk": {
      "sensors": []
    },
    "vis": {
      "sensors": []
    },
    "camera": {
      "sensors": [
        {
          "rho": 0.15765412868951875,
          "theta": 0.3205123685033479,
          "height": 0.18830524507890162,
          "orientation": 0.0,
          "elevation": -29.054604099077228,
          "rangemax": 6.0,
          "panmax": 90.0,
          "tiltmax": 30.0,
          "hfov": 43.6,
          "vfov": 33.4,
          "framerate": 10.0,
          "resolution": "640x480"
        }
      ]
    }
  },
  "bumpers": [],
  "fusionmode": 0,
  "extra": {}
}
