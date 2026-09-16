{
  "name": "quaky",
  "radius": 0.3,
  "icon": [
    {
      "xi": -0.2325,
      "yi": 0.1525,
      "xf": 0.0925,
      "yf": 0.1525
    },
    {
      "xi": -0.2325,
      "yi": -0.1525,
      "xf": 0.0925,
      "yf": -0.1525
    },
    {
      "xi": -0.2325,
      "yi": 0.1525,
      "xf": -0.2325,
      "yf": -0.1525
    },
    {
      "xi": 0.0925,
      "yi": -0.21,
      "xf": 0.0925,
      "yf": 0.21
    },
    {
      "xi": 0.2625,
      "yi": -0.115,
      "xf": 0.2625,
      "yf": 0.115
    },
    {
      "xi": 0.0925,
      "yi": -0.21,
      "xf": 0.1625,
      "yf": -0.21
    },
    {
      "xi": 0.0925,
      "yi": 0.21,
      "xf": 0.1625,
      "yf": 0.21
    },
    {
      "xi": 0.1625,
      "yi": -0.21,
      "xf": 0.2255,
      "yf": -0.18
    },
    {
      "xi": 0.1625,
      "yi": 0.21,
      "xf": 0.2255,
      "yf": 0.18
    },
    {
      "xi": 0.2255,
      "yi": -0.18,
      "xf": 0.2625,
      "yf": -0.115
    },
    {
      "xi": 0.2255,
      "yi": 0.18,
      "xf": 0.2625,
      "yf": 0.115
    }
  ],
  "kinematics": {
    "drive": "tc.vrobot.models.DifferentialDrive",
    "vmax": 0.2,
    "rmax": 130.0,
    "maxmotor": 100.0,
    "maxsteer": 0.0,
    "samax": 0.0,
    "lamax": 0.0,
    "ldmax": 0.0,
    "length": 0.0,
    "base": 0.3625,
    "rwheel": 0.0,
    "wheel": 0.1487,
    "gear": 60.0,
    "pulses": 500.0,
    "dtime": 100,
    "odomET": 0.025,
    "odomER": 0.1,
    "odomBias": 0.1
  },
  "sensors": {
    "son": {
      "rangemax": 5.0,
      "rangemin": 0.135,
      "cone": 20.0,
      "rays": 11,
      "sensors": [
        {
          "rho": 0.253783,
          "theta": 55.8403,
          "height": 0.0,
          "orientation": 90.0,
          "step": 0
        },
        {
          "rho": 0.2759579904623166,
          "theta": 44.522852072591476,
          "height": 0.0,
          "orientation": 60.0,
          "step": 0
        },
        {
          "rho": 0.28537678520674753,
          "theta": 31.23923369493647,
          "height": 0.0,
          "orientation": 30.0,
          "step": 0
        },
        {
          "rho": 0.2682,
          "theta": 11.8336,
          "height": 0.0,
          "orientation": 0.0,
          "step": 0
        },
        {
          "rho": 0.2682,
          "theta": -11.8336,
          "height": 0.0,
          "orientation": 0.0,
          "step": 0
        },
        {
          "rho": 0.28530012059813886,
          "theta": -30.429925248406256,
          "height": 0.0,
          "orientation": -30.0,
          "step": 0
        },
        {
          "rho": 0.2761268289917762,
          "theta": -44.77990987070449,
          "height": 0.0,
          "orientation": -60.0,
          "step": 0
        },
        {
          "rho": 0.253783,
          "theta": -55.8403,
          "height": 0.0,
          "orientation": -90.0,
          "step": 0
        },
        {
          "rho": 0.237828,
          "theta": -140.1173,
          "height": 0.0,
          "orientation": -90.0,
          "step": 0
        },
        {
          "rho": 0.237828,
          "theta": 140.1173,
          "height": 0.0,
          "orientation": 90.0,
          "step": 0
        }
      ]
    },
    "ir": {
      "rangemax": 1.2,
      "rangemin": 0.1,
      "cone": 10.0,
      "rays": 5,
      "sensors": [
        {
          "rho": 0.2768429612219323,
          "theta": 44.48778543370271,
          "height": 0.0,
          "orientation": 60.0,
          "step": 0
        },
        {
          "rho": 0.2853767852067473,
          "theta": 31.239233694936495,
          "height": 0.0,
          "orientation": 30.0,
          "step": 0
        },
        {
          "rho": 0.2682,
          "theta": 11.8336,
          "height": 0.0,
          "orientation": 0.0,
          "step": 0
        },
        {
          "rho": 0.2625,
          "theta": 0.0,
          "height": 0.0,
          "orientation": 0.0,
          "step": 0
        },
        {
          "rho": 0.2682,
          "theta": -11.8336,
          "height": 0.0,
          "orientation": 0.0,
          "step": 0
        },
        {
          "rho": 0.28598454263666606,
          "theta": -30.465563255732164,
          "height": 0.0,
          "orientation": -30.0,
          "step": 0
        },
        {
          "rho": 0.2761250181661291,
          "theta": -44.92663319143499,
          "height": 0.0,
          "orientation": -60.0,
          "step": 0
        }
      ]
    },
    "lrf": {
      "sensors": []
    },
    "lsb": {
      "sensors": []
    },
    "trk": {
      "sensors": []
    },
    "vis": {
      "sensors": []
    }
  },
  "bumpers": [
    {
      "xi": 0.24,
      "yi": 0.195,
      "xf": 0.24,
      "yf": -0.195
    },
    {
      "xi": 0.24,
      "yi": -0.195,
      "xf": -0.23,
      "yf": -0.15
    },
    {
      "xi": -0.23,
      "yi": -0.15,
      "xf": -0.23,
      "yf": 0.15
    },
    {
      "xi": -0.23,
      "yi": 0.15,
      "xf": 0.24,
      "yf": 0.195
    }
  ],
  "extra": {
    "grouplen4": "0.2200",
    "grouplen2": "0.2625",
    "grouplen3": "0.2800",
    "grouplen0": "0.2200",
    "grouplen1": "0.2800",
    "MODEVIRTU": "4",
    "RANGEVIRTU": "5.0",
    "virtumode9": "0",
    "virtumode8": "0",
    "virtumode7": "0",
    "RANGEGROUP": "0.75",
    "virtumode0": "0",
    "CONEVIRTU": "17",
    "FILTERVIRTU": "anfis5.filter",
    "groupfeat0": "90.0",
    "groupmode2": "4",
    "groupmode3": "4",
    "groupmode4": "4",
    "MAXTURN": "10",
    "groupfeat2": "0.0",
    "groupfeat1": "45.0",
    "groupfeat4": "-90.0",
    "groupfeat3": "-45.0",
    "grouprho3": "-45.0",
    "grouprho2": "0.0",
    "MAXGROUP": "5",
    "grouprho4": "-120.0",
    "groupmode0": "4",
    "groupmode1": "4",
    "grouprho1": "45.0",
    "grouprho0": "120.0",
    "MODESON": "2",
    "virturho0": "55.8403",
    "virturho6": "-44.1826",
    "virturho5": "-29.9987",
    "virturho8": "-140.1173",
    "virturho7": "-55.8403",
    "virturho2": "29.9987",
    "virturho1": "44.1826",
    "virturho4": "-11.8336",
    "virturho3": "11.8336",
    "virturho9": "140.1173",
    "SENSIBSON": "0.0000001",
    "ERRORSON": "0.05",
    "MAXSPEED": "1",
    "virtufeat2": "30.0",
    "virtulen0": "0.253783",
    "virtufeat3": "0.0",
    "virtufeat4": "0.0",
    "virtufeat5": "-30.0",
    "virtulen3": "0.268200",
    "virtufeat6": "-60.0",
    "virtulen4": "0.268200",
    "virtufeat7": "-90.0",
    "virtulen1": "0.272617",
    "virtufeat8": "-90.0",
    "virtulen2": "0.280011",
    "virtufeat9": "90.0",
    "virtulen7": "0.253783",
    "virtulen8": "0.237828",
    "virtulen5": "0.280011",
    "virtulen6": "0.272617",
    "RAYVIRTU": "15",
    "virtufeat0": "90.0",
    "virtulen9": "0.237828",
    "virtufeat1": "60.0",
    "ERRORIR": "0.05",
    "MAXVIRTU": "10",
    "MODEIR": "2"
  }
}
