//
//  AudioSpectrumView.swift
//  AIConversationKit
//
//  Created by einhorn on 2024/10/24.
//

import UIKit

class AudioSpectrumView: UIView {
    
    private var specGradientLayer: CAGradientLayer = CAGradientLayer()
    var barWidth: CGFloat = 0.0
    var space: CGFloat = 0.0
    var bottomSpace: CGFloat = 0.0
    var topSpace: CGFloat = 0.0
    var barCount: Int = 0
    var barMinHeight: CGFloat = 0
    var oldSpectra: [Float] = [Float](repeating: -300, count: 256)
    var timer: Timer?
    private var maskLayer: CAShapeLayer?
    var colors: [UIColor]?
    var locations: [NSNumber]?
    private var barViews: [UIView] = []
    convenience init(withBarWidth barWidth: CGFloat,
                     space: CGFloat,
                     bottomSpace: CGFloat,
                     topSpace: CGFloat,
                     barCount: Int,
                     barMinHeight: CGFloat = 0.0,
                     colors: [UIColor],
                     colorLocations: [NSNumber] = [0.0, 1.0]) {
        self.init(frame: .zero)
        self.barWidth = barWidth
        self.space = space
        self.topSpace = topSpace
        self.barCount = barCount
        self.barMinHeight = barMinHeight
        self.colors = colors
        self.locations = colorLocations
        specGradientLayer.colors = colors
        specGradientLayer.locations = colorLocations
        specGradientLayer.startPoint = CGPointMake(0.0, 0.5)
        specGradientLayer.endPoint = CGPointMake(1.0, 0.5)
    
        setupView()
        
    }
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        configInit()
        setupView()
        self.backgroundColor = .clear
    }
    
    required init?(coder: NSCoder) {
        super.init(coder: coder)
        configInit()
        setupView()
        self.backgroundColor = .clear
    }
    
    private func configInit() {
        self.barWidth = 2.0
        self.space = 6.0
        self.bottomSpace = 0
        self.topSpace = 0
    }
    
    private func setupView() {
        
        for _ in 0 ..< barCount {
            let barView = UIView()
            barView.backgroundColor = UIColor.white
            barView.layer.cornerRadius = barWidth/2
            barView.layer.masksToBounds = true
            addSubview(barView)
            barViews.append(barView)
        }
    }
    
    
    override func layoutSubviews() {
        super.layoutSubviews()
    }
    
    func updateAnimation(_ spectra: [Float], withStyle style: ADSpectraStyle) {
        guard spectra.count > 0 else { return }
        
        let count = self.barCount
        
        for i in 0 ..< count {
            let x = CGFloat(i) * (barWidth + space)
            let height = translateAmplitudeToHeight(spectra[i])
            let centerY = bounds.height / 2
            let y = centerY - height / 2
            
            let barView = barViews[i]
            guard let colors = colors else { return }
            guard let locations = locations else { return }
            let position = CGFloat(i) / CGFloat(barViews.count - 1)
            let color = interpolateColor(at: position, colors: colors, locations: locations)
            barView.backgroundColor = color
            UIView.animate(withDuration: 0.2, delay: 0, options: [.curveEaseInOut], animations: {
                barView.frame = CGRect(x: x, y: y, width: self.barWidth, height: height)
            }, completion: { _ in
            })
            
        }
        
    }
    
 
    
    private func interpolateColor(at position: CGFloat, colors: [UIColor], locations: [NSNumber]) -> UIColor {
          for i in 0..<locations.count - 1 {
              let startLocation = CGFloat(truncating: locations[i])
              let endLocation = CGFloat(truncating: locations[i + 1])
              
              if position >= startLocation && position <= endLocation {
                  let startColor = colors[i]
                  let endColor = colors[i + 1]
                  let range = endLocation - startLocation
                  let ratio = (position - startLocation) / range
                  return blendColor(from: startColor, to: endColor, ratio: ratio)
              }
          }
          return colors.last ?? .black
      }

    private func blendColor(from startColor: UIColor, to endColor: UIColor, ratio: CGFloat) -> UIColor {
        var startRed: CGFloat = 0
        var startGreen: CGFloat = 0
        var startBlue: CGFloat = 0
        var startAlpha: CGFloat = 0
        
        var endRed: CGFloat = 0
        var endGreen: CGFloat = 0
        var endBlue: CGFloat = 0
        var endAlpha: CGFloat = 0
        
        startColor.getRed(&startRed, green: &startGreen, blue: &startBlue, alpha: &startAlpha)
        endColor.getRed(&endRed, green: &endGreen, blue: &endBlue, alpha: &endAlpha)
        
        let red = startRed + (endRed - startRed) * ratio
        let green = startGreen + (endGreen - startGreen) * ratio
        let blue = startBlue + (endBlue - startBlue) * ratio
        let alpha = startAlpha + (endAlpha - startAlpha) * ratio
        
        return UIColor(red: red, green: green, blue: blue, alpha: alpha)
    }
    
    func uniformSample(from array: [Float], count: Int) -> [Float] {
        guard count > 0 && !array.isEmpty else { return [] }
        
        let totalElements = array.count
        let step = Double(totalElements - 1) / Double(count - 1)
        var sampledArray: [Float] = []

        for i in 0..<count {
            let index = Int(round(step * Double(i)))
            sampledArray.append(array[index])
        }

        return sampledArray
    }

    func updateSpectra(_ spectra: [Float], withStyle style: ADSpectraStyle) {
        
        updateAnimation(spectra, withStyle: style)
        
    }
    
    // Private method
    private func translateAmplitudeToHeight(_ amplitude: Float) -> CGFloat {
        // let barHeight = CGFloat((pow(10.0, amplitude / 20.0) - 0.000_317) * 2_000.0)
      //  let energy = CGFloat((pow(10.0, amplitude / 20.0) - 0.000_317) * 2_000.0)
        let dbValue = rmsToDecibels(rms: amplitude)
        let barHeight = CGFloat(dbValue / 100) * self.bounds.height
        if barHeight < barMinHeight || barHeight.isNaN {
            return barMinHeight
        }
        return barHeight
    }
    
    func rmsToDecibels(rms: Float) -> Float {
        return 20 * log10(rms)
    }
    
}


