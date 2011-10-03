
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

result = read.csv("error_analysis.csv", header = TRUE, sep = ",")
summary(result)

cond = result$Condition
func = result$f
funccoll = result$c
othercoll = result$cc
other = result$o

chisq.test(func)
chisq.test(funccoll)
chisq.test(othercoll)
chisq.test(other)