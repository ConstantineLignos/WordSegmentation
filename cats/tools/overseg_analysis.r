# Copyright (C) 2010, 2011 Constantine Lignos
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.

result = read.csv("overseg_rates.csv", header = TRUE, sep = ",")
summary(result)
intervals = result$Interval
ratio = result$Ratio
sum = result$Sum

#ratio[ratio==0.0] <- .00001
#ratio <- log(ratio)
summary(ratio)

model <- lm(ratio~intervals)
summary(model)

plot(intervals, ratio)
abline(model$coefficients)
par(mfrow=c(2,2)) 
plot(model)

qqnorm(model$residuals)
qqline(model$residuals)
shapiro.test(model$residuals)
